package com.yourorg.eventdashboard.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsByEventIdAndPhoneNumber(UUID eventId, String phoneNumber);

    Optional<Subscription> findByIdAndPhoneNumber(UUID id, String phoneNumber);

    List<Subscription> findAllByEventIdAndStatus(UUID eventId, SubscriptionStatus status);
}
