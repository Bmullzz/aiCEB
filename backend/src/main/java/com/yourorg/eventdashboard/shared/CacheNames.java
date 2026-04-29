package com.yourorg.eventdashboard.shared;

/**
 * Spring Cache name constants. Always use these instead of inline string literals.
 */
public final class CacheNames {

    public static final String UPCOMING_EVENTS = "upcomingEvents";
    public static final String CATEGORIES      = "categories";
    public static final String EVENT_DETAIL    = "eventDetail";

    private CacheNames() {}
}
