package com.yourorg.eventdashboard.notification;

import com.yourorg.eventdashboard.event.Event;
import com.yourorg.eventdashboard.event.EventRepository;
import com.yourorg.eventdashboard.event.EventStatus;
import com.yourorg.eventdashboard.shared.SmsDeliveryException;
import com.yourorg.eventdashboard.shared.SmsService;
import com.yourorg.eventdashboard.subscription.Subscription;
import com.yourorg.eventdashboard.subscription.SubscriptionRepository;
import com.yourorg.eventdashboard.subscription.SubscriptionStatus;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
public class NotificationScheduler {

    private final EventRepository eventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SmsService smsService;
    private final int reminderWindowMinutes;

    public NotificationScheduler(
            EventRepository eventRepository,
            SubscriptionRepository subscriptionRepository,
            NotificationLogRepository notificationLogRepository,
            SmsService smsService,
            @Value("${app.scheduler.reminder-window-minutes:90}") int reminderWindowMinutes) {
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.smsService = smsService;
        this.reminderWindowMinutes = reminderWindowMinutes;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.interval-ms:60000}")
    public void processReminders() {
        try {
            Instant now = Instant.now();
            Instant windowEnd = now.plus(reminderWindowMinutes, ChronoUnit.MINUTES);

            List<Event> events = eventRepository.findByStatusAndStartTimeBetween(
                    EventStatus.UPCOMING, now, windowEnd);

            log.info("Scheduler run: checking {} events for reminders", events.size());

            int sent = 0;
            int skipped = 0;
            int failed = 0;

            for (Event event : events) {
                MDC.put("eventId", event.getId().toString());
                try {
                    List<Integer> offsets = event.getAlertOffsets();

                    for (int offset : offsets) {
                        Instant reminderTime = event.getStartTime().minus(offset, ChronoUnit.MINUTES);

                        if (now.isBefore(reminderTime)) {
                            skipped++;
                            continue;
                        }

                        List<Subscription> subscriptions = subscriptionRepository
                                .findAllByEventIdAndStatus(event.getId(), SubscriptionStatus.ACTIVE);

                        for (Subscription subscription : subscriptions) {
                            if (notificationLogRepository.existsBySubscriptionIdAndOffsetMinutes(
                                    subscription.getId(), offset)) {
                                skipped++;
                                continue;
                            }

                            NotificationLog log = new NotificationLog(subscription, offset);
                            try {
                                log = notificationLogRepository.save(log);
                            } catch (DataIntegrityViolationException e) {
                                NotificationScheduler.log.warn(
                                        "Duplicate reminder detected, skipping subscription {}",
                                        subscription.getId());
                                skipped++;
                                continue;
                            }

                            String message = String.format(
                                    "Reminder: %s starts in %d minutes at %s. Reply STOP to unsubscribe.",
                                    event.getTitle(), offset, event.getLocation());

                            try {
                                String sid = smsService.send(subscription.getPhoneNumber(), message);
                                log.setDeliveryStatus(DeliveryStatus.SENT);
                                log.setMessageSid(sid);
                                notificationLogRepository.save(log);
                                sent++;
                            } catch (SmsDeliveryException e) {
                                log.setDeliveryStatus(DeliveryStatus.FAILED);
                                notificationLogRepository.save(log);
                                NotificationScheduler.log.warn(
                                        "SMS send failed for subscription {}: {}",
                                        subscription.getId(), e.getMessage());
                                failed++;
                            }
                        }
                    }
                } finally {
                    MDC.remove("eventId");
                }
            }

            log.info("Scheduler run complete: {} sent, {} skipped, {} failed", sent, skipped, failed);

        } catch (Exception e) {
            log.error("Scheduler run aborted due to unexpected error", e);
        }
    }
}
