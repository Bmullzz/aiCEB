package com.yourorg.eventdashboard.shared;

import com.yourorg.eventdashboard.subscription.SubscriptionRequestDto;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@ContextConfiguration(classes = {GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandler.class})
@WithMockUser
class GlobalExceptionHandlerTest {

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/event-not-found")
        void throwEventNotFound() { throw new EventNotFoundException(UUID.fromString("00000000-0000-0000-0000-000000000001")); }

        @GetMapping("/event-cancelled")
        void throwEventCancelled() { throw new EventCancelledException("Event is cancelled."); }

        @GetMapping("/event-in-past")
        void throwEventInPast() { throw new EventInPastException("Event is in the past."); }

        @GetMapping("/category-not-found")
        void throwCategoryNotFound() { throw new CategoryNotFoundException(UUID.fromString("00000000-0000-0000-0000-000000000002")); }

        @GetMapping("/subscription-not-found")
        void throwSubscriptionNotFound() { throw new SubscriptionNotFoundException("Not found."); }

        @GetMapping("/already-subscribed")
        void throwAlreadySubscribed() { throw new AlreadySubscribedException("Already subscribed."); }

        @GetMapping("/already-opted-out")
        void throwAlreadyOptedOut() { throw new AlreadyOptedOutException("Already opted out."); }

        @GetMapping("/invalid-phone")
        void throwInvalidPhone() { throw new InvalidPhoneNumberException("Invalid phone."); }

        @GetMapping("/invalid-event-time")
        void throwInvalidEventTime() { throw new InvalidEventTimeException("End time must be after start time."); }

        @GetMapping("/sms-delivery")
        void throwSmsDelivery() { throw new SmsDeliveryException("Twilio error."); }

        @GetMapping("/data-integrity")
        void throwDataIntegrity() { throw new DataIntegrityViolationException("duplicate key"); }

        @GetMapping("/access-denied")
        void throwAccessDenied() { throw new AccessDeniedException("Denied."); }

        @GetMapping("/unexpected")
        void throwUnexpected() { throw new RuntimeException("internal secret"); }

        @PostMapping("/validation")
        void requireValid(@Valid @RequestBody SubscriptionRequestDto dto) {}
    }

    @Autowired MockMvc mockMvc;

    @Test
    void eventNotFound_returns404_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/event-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void eventCancelled_returns422_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/event-cancelled"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EVENT_CANCELLED"));
    }

    @Test
    void eventInPast_returns422_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/event-in-past"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EVENT_IN_PAST"));
    }

    @Test
    void categoryNotFound_returns404_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/category-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void subscriptionNotFound_returns404_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/subscription-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }

    @Test
    void alreadySubscribed_returns409_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/already-subscribed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_SUBSCRIBED"));
    }

    @Test
    void alreadyOptedOut_returns409_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/already-opted-out"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_OPTED_OUT"));
    }

    @Test
    void invalidPhoneNumber_returns400_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/invalid-phone"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE_NUMBER"));
    }

    @Test
    void invalidEventTime_returns400_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/invalid-event-time"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EVENT_TIME"));
    }

    @Test
    void smsDelivery_returns502_withCorrectCode() throws Exception {
        mockMvc.perform(get("/test/sms-delivery"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SMS_DELIVERY_FAILED"));
    }

    @Test
    void dataIntegrity_returns409_withConflictCode() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void accessDenied_returns403_withForbiddenCode() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unexpectedException_returns500_withGenericMessage() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }

    @Test
    void validationFailure_returns400_withFieldNameInMessage() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"not-e164\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("phoneNumber")));
    }
}
