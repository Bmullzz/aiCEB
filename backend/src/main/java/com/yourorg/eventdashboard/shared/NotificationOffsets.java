package com.yourorg.eventdashboard.shared;

/**
 * Magic-number constants for the offset_minutes column in notification_log.
 * Positive values are reminder minutes-before-start.
 * Non-positive values identify lifecycle notifications.
 */
public final class NotificationOffsets {

    public static final int CONFIRMATION = 0;
    public static final int CANCELLATION = -1;
    public static final int RESCHEDULE   = -2;

    private NotificationOffsets() {}
}
