package com.yourorg.eventdashboard.notification;

import com.yourorg.eventdashboard.event.Event;
import com.yourorg.eventdashboard.event.EventRepository;
import com.yourorg.eventdashboard.shared.NotificationOffsets;
import com.yourorg.eventdashboard.shared.SmsDeliveryException;
import com.yourorg.eventdashboard.shared.SmsService;
import com.yourorg.eventdashboard.subscription.Subscription;
import com.yourorg.eventdashboard.subscription.SubscriptionRepository;
import com.yourorg.eventdashboard.subscription.SubscriptionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
// Not final — @Async requires CGLIB subclassing for proxy generation
public class NotificationService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a z").withZone(ZoneId.of("UTC"));

    private final EventRepository eventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SmsService smsService;

    public NotificationService(
            EventRepository eventRepository,
            SubscriptionRepository subscriptionRepository,
            NotificationLogRepository notificationLogRepository,
            SmsService smsService) {
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.smsService = smsService;
    }

    @Async
    public CompletableFuture<Void> sendCancellationSms(UUID eventId, String reason) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.warn("sendCancellationSms: event {} not found, skipping batch", eventId);
                return CompletableFuture.completedFuture(null);
            }

            List<Subscription> subscriptions = subscriptionRepository
                    .findAllByEventIdAndStatus(eventId, SubscriptionStatus.ACTIVE);

            String dateStr = TIME_FORMATTER.format(event.getStartTime());
            String reasonClause = (reason != null && !reason.isBlank()) ? "Reason: " + reason + ". " : "";
            String message = "Update: " + event.getTitle() + " scheduled for " + dateStr
                    + " has been cancelled. " + reasonClause + "We're sorry for the inconvenience.";

            int sent = 0;
            int failed = 0;

            for (Subscription subscription : subscriptions) {
                NotificationLog notifLog = new NotificationLog(subscription, NotificationOffsets.CANCELLATION);
                notifLog = notificationLogRepository.save(notifLog);

                try {
                    String sid = smsService.send(subscription.getPhoneNumber(), message);
                    notifLog.setDeliveryStatus(DeliveryStatus.SENT);
                    notifLog.setMessageSid(sid);
                    notificationLogRepository.save(notifLog);
                    sent++;
                } catch (SmsDeliveryException e) {
                    notifLog.setDeliveryStatus(DeliveryStatus.FAILED);
                    notificationLogRepository.save(notifLog);
                    log.warn("Cancellation SMS failed for subscription {}: {}",
                            subscription.getId(), e.getMessage());
                    failed++;
                }
            }

            log.info("Cancellation SMS batch complete for event {}: {} sent, {} failed",
                    eventId, sent, failed);

        } catch (Exception e) {
            log.error("sendCancellationSms failed for event {}", eventId, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> sendRescheduleSms(UUID eventId, Instant oldStartTime, Instant newStartTime) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.warn("sendRescheduleSms: event {} not found, skipping batch", eventId);
                return CompletableFuture.completedFuture(null);
            }

            List<Subscription> subscriptions = subscriptionRepository
                    .findAllByEventIdAndStatus(eventId, SubscriptionStatus.ACTIVE);

            String newTimeStr = TIME_FORMATTER.format(newStartTime);
            String message = "Update: " + event.getTitle() + " has been rescheduled. New time: "
                    + newTimeStr + " at " + event.getLocation()
                    + ". Your reminders have been updated automatically.";

            int sent = 0;
            int failed = 0;

            for (Subscription subscription : subscriptions) {
                NotificationLog notifLog = new NotificationLog(subscription, NotificationOffsets.RESCHEDULE);
                notifLog = notificationLogRepository.save(notifLog);

                try {
                    String sid = smsService.send(subscription.getPhoneNumber(), message);
                    notifLog.setDeliveryStatus(DeliveryStatus.SENT);
                    notifLog.setMessageSid(sid);
                    notificationLogRepository.save(notifLog);
                    sent++;
                } catch (SmsDeliveryException e) {
                    notifLog.setDeliveryStatus(DeliveryStatus.FAILED);
                    notificationLogRepository.save(notifLog);
                    log.warn("Reschedule SMS failed for subscription {}: {}",
                            subscription.getId(), e.getMessage());
                    failed++;
                }
            }

            if (!subscriptions.isEmpty()) {
                List<UUID> subscriptionIds = subscriptions.stream()
                        .map(Subscription::getId)
                        .toList();
                notificationLogRepository.deleteBySubscriptionIdInAndOffsetMinutesGreaterThan(
                        subscriptionIds, 0);
            }

            log.info("Reschedule SMS batch complete for event {}: {} sent, {} failed",
                    eventId, sent, failed);

        } catch (Exception e) {
            log.error("sendRescheduleSms failed for event {}", eventId, e);
        }

        return CompletableFuture.completedFuture(null);
    }
}
