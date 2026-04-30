package com.yourorg.eventdashboard.notification;

import com.yourorg.eventdashboard.subscription.Subscription;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Does NOT extend BaseEntity. The notification_log table has sentAt/updatedAt as domain
 * fields (not standard audit columns), and has no created_at column, so BaseEntity
 * inheritance is intentionally omitted here.
 */
@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "offset_minutes", nullable = false)
    private int offsetMinutes;

    @Column(name = "message_sid", length = 64)
    private String messageSid;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private DeliveryStatus deliveryStatus = DeliveryStatus.QUEUED;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onPersist() {
        if (sentAt == null) sentAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public NotificationLog(Subscription subscription, int offsetMinutes) {
        this.subscription = subscription;
        this.offsetMinutes = offsetMinutes;
    }
}
