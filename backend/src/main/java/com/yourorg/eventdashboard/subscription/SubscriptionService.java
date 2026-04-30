package com.yourorg.eventdashboard.subscription;

import com.yourorg.eventdashboard.event.Event;
import com.yourorg.eventdashboard.event.EventRepository;
import com.yourorg.eventdashboard.event.EventStatus;
import com.yourorg.eventdashboard.shared.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final SmsService smsService;
    private final String baseUrl;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            EventRepository eventRepository,
            SmsService smsService,
            @Value("${app.base-url}") String baseUrl) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.smsService = smsService;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public SubscriptionResponseDto subscribe(UUID eventId, String rawPhoneNumber) {
        String phoneNumber = PhoneNumberValidator.toE164(rawPhoneNumber);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new EventCancelledException("Event '" + event.getTitle() + "' has been cancelled.");
        }

        if (event.getStartTime().isBefore(Instant.now())) {
            throw new EventInPastException("Event '" + event.getTitle() + "' is in the past.");
        }

        if (subscriptionRepository.existsByEventIdAndPhoneNumber(eventId, phoneNumber)) {
            throw new AlreadySubscribedException("Phone number is already subscribed to this event.");
        }

        Subscription subscription = new Subscription(event, phoneNumber);
        subscription = subscriptionRepository.save(subscription);

        String optOutUrl = baseUrl + "/api/subscriptions/opt-out";
        String message = "You're subscribed to \"" + event.getTitle() + "\"! "
                + "You'll receive reminders before the event. "
                + "To opt out, visit: " + optOutUrl;

        try {
            smsService.send(phoneNumber, message);
        } catch (SmsDeliveryException e) {
            log.warn("Confirmation SMS failed for subscription {}: {}", subscription.getId(), e.getMessage());
        }

        return toResponseDto(subscription);
    }

    @Transactional
    public SubscriptionResponseDto optOut(UUID subscriptionId, String rawPhoneNumber) {
        String phoneNumber = PhoneNumberValidator.toE164(rawPhoneNumber);

        Subscription subscription = subscriptionRepository.findByIdAndPhoneNumber(subscriptionId, phoneNumber)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Subscription not found for the provided ID and phone number."));

        if (subscription.getStatus() == SubscriptionStatus.OPTED_OUT) {
            throw new AlreadyOptedOutException("This subscription is already opted out.");
        }

        subscription.setStatus(SubscriptionStatus.OPTED_OUT);
        subscription = subscriptionRepository.save(subscription);

        return toResponseDto(subscription);
    }

    private SubscriptionResponseDto toResponseDto(Subscription subscription) {
        return new SubscriptionResponseDto(
                subscription.getId(),
                subscription.getEvent().getTitle(),
                subscription.getPhoneNumber(),
                subscription.getEvent().getAlertOffsets(),
                subscription.getStatus().name()
        );
    }
}
