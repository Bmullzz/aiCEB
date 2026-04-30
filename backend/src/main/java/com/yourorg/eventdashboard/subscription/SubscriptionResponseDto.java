package com.yourorg.eventdashboard.subscription;

import java.util.List;
import java.util.UUID;

public record SubscriptionResponseDto(
        UUID subscriptionId,
        String eventTitle,
        String phoneNumber,
        List<Integer> alertOffsets,
        String status
) {}
