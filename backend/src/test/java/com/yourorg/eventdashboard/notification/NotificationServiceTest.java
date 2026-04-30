package com.yourorg.eventdashboard.notification;

import com.yourorg.eventdashboard.event.Event;
import com.yourorg.eventdashboard.event.EventRepository;
import com.yourorg.eventdashboard.shared.NotificationOffsets;
import com.yourorg.eventdashboard.shared.SmsDeliveryException;
import com.yourorg.eventdashboard.shared.SmsService;
import com.yourorg.eventdashboard.subscription.Subscription;
import com.yourorg.eventdashboard.subscription.SubscriptionRepository;
import com.yourorg.eventdashboard.subscription.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private SmsService smsService;

    private NotificationService service;

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SUB_ID = UUID.randomUUID();
    private static final Instant FUTURE = Instant.now().plus(7, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                eventRepository, subscriptionRepository, notificationLogRepository, smsService);
    }

    private Event mockEvent() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getTitle()).thenReturn("Test Event");
        when(event.getLocation()).thenReturn("Room 101");
        when(event.getStartTime()).thenReturn(FUTURE);
        return event;
    }

    private Subscription mockSubscription() {
        Subscription sub = mock(Subscription.class);
        when(sub.getId()).thenReturn(SUB_ID);
        when(sub.getPhoneNumber()).thenReturn("+14155552671");
        return sub;
    }

    // ─── sendCancellationSms ──────────────────────────────────────────────────

    @Test
    void sendCancellationSms_sendsToAllActiveSubscribers() {
        Event event = mockEvent();
        Subscription sub = mockSubscription();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(anyString(), anyString())).thenReturn("SID-001");

        CompletableFuture<Void> future = service.sendCancellationSms(EVENT_ID, null);

        assertThat(future).isNotNull();
        verify(smsService).send(eq("+14155552671"), anyString());
    }

    @Test
    void sendCancellationSms_includesReason_whenNotBlank() {
        Event event = mockEvent();
        Subscription sub = mockSubscription();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(anyString(), anyString())).thenReturn("SID-002");

        service.sendCancellationSms(EVENT_ID, "Venue unavailable");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).send(anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("Reason: Venue unavailable");
    }

    @Test
    void sendCancellationSms_omitsReason_whenBlank() {
        Event event = mockEvent();
        Subscription sub = mockSubscription();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(anyString(), anyString())).thenReturn("SID-003");

        service.sendCancellationSms(EVENT_ID, "  ");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).send(anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).doesNotContain("Reason:");
    }

    @Test
    void sendCancellationSms_continuesAfterOneSmsFailure() {
        Event event = mockEvent();
        UUID sub1Id = UUID.randomUUID();
        UUID sub2Id = UUID.randomUUID();
        Subscription sub1 = mock(Subscription.class);
        Subscription sub2 = mock(Subscription.class);
        when(sub1.getId()).thenReturn(sub1Id);
        when(sub1.getPhoneNumber()).thenReturn("+14155550011");
        when(sub2.getId()).thenReturn(sub2Id);
        when(sub2.getPhoneNumber()).thenReturn("+14155550022");

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub1, sub2));
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(eq("+14155550011"), anyString()))
                .thenThrow(new SmsDeliveryException("Twilio error"));
        when(smsService.send(eq("+14155550022"), anyString())).thenReturn("SID-004");

        CompletableFuture<Void> future = service.sendCancellationSms(EVENT_ID, null);

        assertThat(future).isNotNull();
        verify(smsService).send(eq("+14155550011"), anyString());
        verify(smsService).send(eq("+14155550022"), anyString());
    }

    @Test
    void sendCancellationSms_returnsCompletedFuture_whenEventNotFound() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        CompletableFuture<Void> future = service.sendCancellationSms(EVENT_ID, null);

        assertThat(future).isNotNull();
        assertThat(future.isDone()).isTrue();
        verify(smsService, never()).send(anyString(), anyString());
    }

    // ─── sendRescheduleSms ────────────────────────────────────────────────────

    @Test
    void sendRescheduleSms_sendsToAllActiveSubscribers_withNewTimeInMessage() {
        Event event = mockEvent();
        Instant newTime = FUTURE.plus(1, ChronoUnit.HOURS);
        Subscription sub = mockSubscription();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(anyString(), anyString())).thenReturn("SID-005");

        service.sendRescheduleSms(EVENT_ID, FUTURE, newTime);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).send(anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("rescheduled");
    }

    @Test
    void sendRescheduleSms_deletesExistingScheduledLogs_afterSending() {
        Event event = mockEvent();
        Instant newTime = FUTURE.plus(1, ChronoUnit.HOURS);
        Subscription sub = mockSubscription();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(anyString(), anyString())).thenReturn("SID-006");

        service.sendRescheduleSms(EVENT_ID, FUTURE, newTime);

        verify(notificationLogRepository).deleteBySubscriptionIdInAndOffsetMinutesGreaterThan(
                argThat(ids -> ids.contains(SUB_ID)), eq(0));
    }

    @Test
    void sendRescheduleSms_returnsCompletedFuture_whenEventNotFound() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        CompletableFuture<Void> future = service.sendRescheduleSms(EVENT_ID, FUTURE, FUTURE);

        assertThat(future).isNotNull();
        assertThat(future.isDone()).isTrue();
        verify(smsService, never()).send(anyString(), anyString());
    }
}
