package com.yourorg.eventdashboard.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.eventdashboard.shared.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubscriptionController.class)
@ContextConfiguration(classes = {SubscriptionController.class, GlobalExceptionHandler.class})
@WithMockUser
class SubscriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SubscriptionService subscriptionService;

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SUB_ID = UUID.randomUUID();
    private static final String PHONE = "+14155552671";

    private SubscriptionResponseDto sampleResponse(String status) {
        return new SubscriptionResponseDto(SUB_ID, "Test Event", PHONE, List.of(60), status);
    }

    @Test
    void subscribe_returns201_onSuccess() throws Exception {
        when(subscriptionService.subscribe(eq(EVENT_ID), eq(PHONE))).thenReturn(sampleResponse("ACTIVE"));

        var request = new SubscriptionRequestDto(EVENT_ID, PHONE);
        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subscriptionId").value(SUB_ID.toString()));
    }

    @Test
    void subscribe_returns400_whenPhoneMissing() throws Exception {
        var request = new SubscriptionRequestDto(EVENT_ID, null);
        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void subscribe_returns400_whenEventIdMissing() throws Exception {
        var request = new SubscriptionRequestDto(null, PHONE);
        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void subscribe_returns404_whenEventNotFound() throws Exception {
        when(subscriptionService.subscribe(any(), any()))
                .thenThrow(new EventNotFoundException(EVENT_ID));

        var request = new SubscriptionRequestDto(EVENT_ID, PHONE);
        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    @Test
    void subscribe_returns409_whenAlreadySubscribed() throws Exception {
        when(subscriptionService.subscribe(any(), any()))
                .thenThrow(new AlreadySubscribedException("Already subscribed."));

        var request = new SubscriptionRequestDto(EVENT_ID, PHONE);
        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_SUBSCRIBED"));
    }

    @Test
    void subscribe_returns422_whenEventInPast() throws Exception {
        when(subscriptionService.subscribe(any(), any()))
                .thenThrow(new EventInPastException("Event is in the past."));

        var request = new SubscriptionRequestDto(EVENT_ID, PHONE);
        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EVENT_IN_PAST"));
    }

    @Test
    void optOut_returns200_onSuccess() throws Exception {
        when(subscriptionService.optOut(eq(SUB_ID), eq(PHONE))).thenReturn(sampleResponse("OPTED_OUT"));

        var request = new OptOutRequestDto(SUB_ID, PHONE);
        mockMvc.perform(post("/api/subscriptions/opt-out")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPTED_OUT"));
    }

    @Test
    void optOut_returns404_whenSubscriptionNotFound() throws Exception {
        when(subscriptionService.optOut(any(), any()))
                .thenThrow(new SubscriptionNotFoundException("Not found."));

        var request = new OptOutRequestDto(SUB_ID, PHONE);
        mockMvc.perform(post("/api/subscriptions/opt-out")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }

    @Test
    void optOut_returns409_whenAlreadyOptedOut() throws Exception {
        when(subscriptionService.optOut(any(), any()))
                .thenThrow(new AlreadyOptedOutException("Already opted out."));

        var request = new OptOutRequestDto(SUB_ID, PHONE);
        mockMvc.perform(post("/api/subscriptions/opt-out")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_OPTED_OUT"));
    }

    @Test
    void subscribe_returns400_withFieldNameInMessage_whenPhoneInvalidFormat() throws Exception {
        // Phone number present but fails E.164 @Pattern — verifies message contains field name
        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"" + EVENT_ID + "\",\"phoneNumber\":\"not-e164\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("phoneNumber")));
    }
}
