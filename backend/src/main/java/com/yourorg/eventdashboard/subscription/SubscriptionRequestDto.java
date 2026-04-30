package com.yourorg.eventdashboard.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record SubscriptionRequestDto(
        @NotNull(message = "eventId is required")
        UUID eventId,

        @NotBlank(message = "phoneNumber is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "phoneNumber must be a valid phone number")
        String phoneNumber
) {}
