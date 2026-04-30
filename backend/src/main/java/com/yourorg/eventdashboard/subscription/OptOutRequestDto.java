package com.yourorg.eventdashboard.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OptOutRequestDto(
        @NotNull(message = "subscriptionId is required")
        UUID subscriptionId,

        @NotBlank(message = "phoneNumber is required")
        String phoneNumber
) {}
