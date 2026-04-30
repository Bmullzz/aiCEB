package com.yourorg.eventdashboard.subscription;

import com.yourorg.eventdashboard.event.Event;
import com.yourorg.eventdashboard.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "subscription")
@Getter
@Setter
@NoArgsConstructor
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private Instant subscribedAt;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        subscribedAt = Instant.now();
    }

    public Subscription(Event event, String phoneNumber) {
        this.event = event;
        this.phoneNumber = phoneNumber;
    }
}
