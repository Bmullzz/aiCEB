package com.yourorg.eventdashboard.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStatusAndStartTimeBetween(EventStatus status, Instant from, Instant to);
}
