package com.yourorg.eventdashboard.subscription;

import com.yourorg.eventdashboard.event.Event;
import com.yourorg.eventdashboard.event.EventRepository;
import com.yourorg.eventdashboard.event.EventStatus;
import com.yourorg.eventdashboard.shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private EventRepository eventRepository;
    @Mock private SmsService smsService;

    private SubscriptionService service;

    private static final String BASE_URL = "https://example.com";
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final String RAW_PHONE = "+14155552671";
    private static final String E164_PHONE = "+14155552671";

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptionRepository, eventRepository, smsService, BASE_URL);
    }

    private Event futureEvent() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getTitle()).thenReturn("Test Event");
        when(event.getStatus()).thenReturn(EventStatus.UPCOMING);
        when(event.getStartTime()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
        when(event.getAlertOffsets()).thenReturn(java.util.List.of(60));
        return event;
    }

    @Test
    void subscribe_returnsDto_onSuccess() {
        Event event = futureEvent();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.existsByEventIdAndPhoneNumber(EVENT_ID, E164_PHONE)).thenReturn(false);

        Subscription saved = mock(Subscription.class);
        when(saved.getId()).thenReturn(UUID.randomUUID());
        when(saved.getPhoneNumber()).thenReturn(E164_PHONE);
        when(saved.getStatus()).thenReturn(SubscriptionStatus.ACTIVE);
        when(saved.getEvent()).thenReturn(event);
        when(subscriptionRepository.save(any())).thenReturn(saved);

        SubscriptionResponseDto result = service.subscribe(EVENT_ID, RAW_PHONE);

        assertThat(result.phoneNumber()).isEqualTo(E164_PHONE);
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(smsService).send(eq(E164_PHONE), anyString());
    }

    @Test
    void subscribe_throwsEventNotFoundException_whenEventMissing() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(EVENT_ID, RAW_PHONE))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void subscribe_throwsEventCancelledException_whenCancelled() {
        Event event = mock(Event.class);
        when(event.getStatus()).thenReturn(EventStatus.CANCELLED);
        when(event.getTitle()).thenReturn("Cancelled Event");
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.subscribe(EVENT_ID, RAW_PHONE))
                .isInstanceOf(EventCancelledException.class);
    }

    @Test
    void subscribe_throwsEventInPastException_whenPast() {
        Event event = mock(Event.class);
        when(event.getStatus()).thenReturn(EventStatus.UPCOMING);
        when(event.getTitle()).thenReturn("Past Event");
        when(event.getStartTime()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.subscribe(EVENT_ID, RAW_PHONE))
                .isInstanceOf(EventInPastException.class);
    }

    @Test
    void subscribe_throwsAlreadySubscribedException_whenDuplicate() {
        Event event = futureEvent();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.existsByEventIdAndPhoneNumber(EVENT_ID, E164_PHONE)).thenReturn(true);

        assertThatThrownBy(() -> service.subscribe(EVENT_ID, RAW_PHONE))
                .isInstanceOf(AlreadySubscribedException.class);
    }

    @Test
    void subscribe_stillCommits_whenSmsFails() {
        Event event = futureEvent();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.existsByEventIdAndPhoneNumber(EVENT_ID, E164_PHONE)).thenReturn(false);

        Subscription saved = mock(Subscription.class);
        when(saved.getId()).thenReturn(UUID.randomUUID());
        when(saved.getPhoneNumber()).thenReturn(E164_PHONE);
        when(saved.getStatus()).thenReturn(SubscriptionStatus.ACTIVE);
        when(saved.getEvent()).thenReturn(event);
        when(subscriptionRepository.save(any())).thenReturn(saved);
        doThrow(new SmsDeliveryException("SMS failed")).when(smsService).send(anyString(), anyString());

        SubscriptionResponseDto result = service.subscribe(EVENT_ID, RAW_PHONE);

        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(any());
    }

    @Test
    void optOut_returnsDto_onSuccess() {
        UUID subId = UUID.randomUUID();
        Event event = futureEvent();

        Subscription sub = mock(Subscription.class);
        when(sub.getId()).thenReturn(subId);
        when(sub.getPhoneNumber()).thenReturn(E164_PHONE);
        when(sub.getStatus()).thenReturn(SubscriptionStatus.ACTIVE);
        when(sub.getEvent()).thenReturn(event);
        when(subscriptionRepository.findByIdAndPhoneNumber(subId, E164_PHONE)).thenReturn(Optional.of(sub));

        Subscription updated = mock(Subscription.class);
        when(updated.getId()).thenReturn(subId);
        when(updated.getPhoneNumber()).thenReturn(E164_PHONE);
        when(updated.getStatus()).thenReturn(SubscriptionStatus.OPTED_OUT);
        when(updated.getEvent()).thenReturn(event);
        when(subscriptionRepository.save(sub)).thenReturn(updated);

        SubscriptionResponseDto result = service.optOut(subId, RAW_PHONE);

        assertThat(result.status()).isEqualTo("OPTED_OUT");
    }

    @Test
    void optOut_throwsSubscriptionNotFoundException_whenNotFound() {
        UUID subId = UUID.randomUUID();
        when(subscriptionRepository.findByIdAndPhoneNumber(subId, E164_PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.optOut(subId, RAW_PHONE))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void optOut_throwsAlreadyOptedOutException_whenAlreadyOptedOut() {
        UUID subId = UUID.randomUUID();
        Event event = futureEvent();

        Subscription sub = mock(Subscription.class);
        when(sub.getStatus()).thenReturn(SubscriptionStatus.OPTED_OUT);
        when(subscriptionRepository.findByIdAndPhoneNumber(subId, E164_PHONE)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.optOut(subId, RAW_PHONE))
                .isInstanceOf(AlreadyOptedOutException.class);
    }

    @Test
    void subscribe_throwsInvalidPhoneNumberException_forBadPhone() {
        assertThatThrownBy(() -> service.subscribe(EVENT_ID, "not-a-phone"))
                .isInstanceOf(InvalidPhoneNumberException.class);
        verifyNoInteractions(eventRepository);
    }
}
