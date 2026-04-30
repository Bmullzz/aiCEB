package com.yourorg.eventdashboard.subscription;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponseDto> subscribe(@Valid @RequestBody SubscriptionRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.subscribe(request.eventId(), request.phoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/opt-out")
    public ResponseEntity<SubscriptionResponseDto> optOut(@Valid @RequestBody OptOutRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.optOut(request.subscriptionId(), request.phoneNumber());
        return ResponseEntity.ok(response);
    }
}
