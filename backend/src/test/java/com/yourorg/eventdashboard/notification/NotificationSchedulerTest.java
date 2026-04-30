package com.yourorg.eventdashboard.notification;

import com.yourorg.eventdashboard.event.Event;
import com.yourorg.eventdashboard.event.EventRepository;
import com.yourorg.eventdashboard.event.EventStatus;
import com.yourorg.eventdashboard.shared.SmsDeliveryException;
import com.yourorg.eventdashboard.shared.SmsService;
import com.yourorg.eventdashboard.subscription.Subscription;
import com.yourorg.eventdashboard.subscription.SubscriptionRepository;
import com.yourorg.eventdashboard.subscription.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationSchedulerTest {

    @Mock private EventRepository eventRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private SmsService smsService;

    private NotificationScheduler scheduler;

    private static final int WINDOW_MINUTES = 90;
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SUB_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        scheduler = new NotificationScheduler(
                eventRepository, subscriptionRepository,
                notificationLogRepository, smsService, WINDOW_MINUTES);
    }

    private Event eventStartingIn(int minutesFromNow, int offset, Integer offset2) {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getTitle()).thenReturn("Test Event");
        when(event.getLocation()).thenReturn("Room 101");
        when(event.getStatus()).thenReturn(EventStatus.UPCOMING);
        when(event.getStartTime()).thenReturn(Instant.now().plus(minutesFromNow, ChronoUnit.MINUTES));
        List<Integer> offsets = offset2 != null ? List.of(offset, offset2) : List.of(offset);
        when(event.getAlertOffsets()).thenReturn(offsets);
        return event;
    }

    private Subscription activeSubscription() {
        Subscription sub = mock(Subscription.class);
        when(sub.getId()).thenReturn(SUB_ID);
        when(sub.getPhoneNumber()).thenReturn("+14155552671");
        return sub;
    }

    private NotificationLog savedLog() {
        NotificationLog log = new NotificationLog();
        return log;
    }

    @Test
    void sendsReminder_whenEventInWindowAndNoLogExists() {
        // Event starts in 30 min, 60-min offset → reminderTime = now - 30 min (past due)
        Event event = eventStartingIn(30, 60, null);
        when(eventRepository.findByStatusAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(event));
        Subscription sub = activeSubscription();
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.existsBySubscriptionIdAndOffsetMinutes(SUB_ID, 60))
                .thenReturn(false);
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(anyString(), anyString())).thenReturn("SID-123");

        scheduler.processReminders();

        verify(smsService).send(eq("+14155552671"), anyString());
        verify(notificationLogRepository, times(2)).save(any()); // once QUEUED, once SENT
    }

    @Test
    void skipsReminder_whenLogRowAlreadyExists() {
        Event event = eventStartingIn(30, 60, null);
        when(eventRepository.findByStatusAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(event));
        Subscription sub = activeSubscription();
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.existsBySubscriptionIdAndOffsetMinutes(SUB_ID, 60))
                .thenReturn(true);

        scheduler.processReminders();

        verify(smsService, never()).send(anyString(), anyString());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void skipsReminder_whenCurrentTimeIsBeforeReminderTime() {
        // Event starts in 90 min, 60-min offset → reminderTime = now + 30 min (not yet due)
        Event event = eventStartingIn(90, 60, null);
        when(eventRepository.findByStatusAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(event));

        scheduler.processReminders();

        verify(subscriptionRepository, never()).findAllByEventIdAndStatus(any(), any());
        verify(smsService, never()).send(anyString(), anyString());
    }

    @Test
    void continuesProcessing_afterOneSmsFailure() {
        Event event = eventStartingIn(30, 60, null);
        when(eventRepository.findByStatusAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(event));

        UUID sub1Id = UUID.randomUUID();
        UUID sub2Id = UUID.randomUUID();
        Subscription sub1 = mock(Subscription.class);
        Subscription sub2 = mock(Subscription.class);
        when(sub1.getId()).thenReturn(sub1Id);
        when(sub1.getPhoneNumber()).thenReturn("+14155550001");
        when(sub2.getId()).thenReturn(sub2Id);
        when(sub2.getPhoneNumber()).thenReturn("+14155550002");

        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub1, sub2));
        when(notificationLogRepository.existsBySubscriptionIdAndOffsetMinutes(any(UUID.class), anyInt()))
                .thenReturn(false);
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(eq("+14155550001"), anyString()))
                .thenThrow(new SmsDeliveryException("Twilio error"));
        when(smsService.send(eq("+14155550002"), anyString())).thenReturn("SID-456");

        scheduler.processReminders();

        verify(smsService).send(eq("+14155550001"), anyString());
        verify(smsService).send(eq("+14155550002"), anyString());
    }

    @Test
    void handlesDataIntegrityViolation_gracefully() {
        Event event = eventStartingIn(30, 60, null);
        when(eventRepository.findByStatusAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(event));
        Subscription sub = activeSubscription();
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.existsBySubscriptionIdAndOffsetMinutes(SUB_ID, 60))
                .thenReturn(false);
        when(notificationLogRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatNoException().isThrownBy(() -> scheduler.processReminders());
        verify(smsService, never()).send(anyString(), anyString());
    }

    @Test
    void doesNotThrow_whenEventRepositoryThrows() {
        when(eventRepository.findByStatusAndStartTimeBetween(any(), any(), any()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("DB down"));

        assertThatNoException().isThrownBy(() -> scheduler.processReminders());
        verify(smsService, never()).send(anyString(), anyString());
    }

    @Test
    void processesBothAlertOffsets_whenBothSet() {
        // Event starts in 10 min, offsets 60 and 15 — both reminder times are past due
        Event event = eventStartingIn(10, 60, 15);
        when(eventRepository.findByStatusAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(event));
        Subscription sub = activeSubscription();
        when(subscriptionRepository.findAllByEventIdAndStatus(EVENT_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(notificationLogRepository.existsBySubscriptionIdAndOffsetMinutes(any(), anyInt()))
                .thenReturn(false);
        when(notificationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.send(anyString(), anyString())).thenReturn("SID-789");

        scheduler.processReminders();

        // SMS sent twice — once for each offset
        verify(smsService, times(2)).send(anyString(), anyString());
    }
}
