package com.yourorg.eventdashboard.event;

import com.yourorg.eventdashboard.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
public class Event extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.UPCOMING;

    @Column(nullable = false)
    private int alertOffsetMinutes = 60;

    @Column(name = "alert_offset_minutes_2")
    private Integer alertOffsetMinutes2;

    @Column(nullable = false)
    private boolean visible = true;

    /** Returns the configured reminder offsets in order, omitting the second if null. */
    public List<Integer> getAlertOffsets() {
        List<Integer> offsets = new ArrayList<>();
        offsets.add(alertOffsetMinutes);
        if (alertOffsetMinutes2 != null) {
            offsets.add(alertOffsetMinutes2);
        }
        return offsets;
    }
}
