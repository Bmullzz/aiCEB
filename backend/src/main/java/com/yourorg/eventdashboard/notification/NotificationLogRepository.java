package com.yourorg.eventdashboard.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsBySubscriptionIdAndOffsetMinutes(UUID subscriptionId, int offsetMinutes);

    List<NotificationLog> findByDeliveryStatusAndSentAtAfter(DeliveryStatus status, Instant cutoff);

    Optional<NotificationLog> findByMessageSid(String messageSid);

    void deleteBySubscriptionIdInAndOffsetMinutesGreaterThan(List<UUID> subscriptionIds, int threshold);
}
