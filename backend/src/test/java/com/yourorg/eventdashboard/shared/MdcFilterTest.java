package com.yourorg.eventdashboard.shared;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MdcFilterTest.MdcTestController.class)
@ContextConfiguration(classes = {MdcFilterTest.MdcTestController.class, MdcFilter.class, GlobalExceptionHandler.class})
@WithMockUser
class MdcFilterTest {

    @RestController
    @RequestMapping("/test")
    static class MdcTestController {

        @GetMapping("/mdc-request-id")
        public String getRequestId() {
            String requestId = MDC.get("requestId");
            return requestId != null ? requestId : "null";
        }
    }

    @Autowired MockMvc mockMvc;

    @Test
    void requestId_isPresentInMdcDuringRequestProcessing() throws Exception {
        String body = mockMvc.perform(get("/test/mdc-request-id"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // The controller reads MDC.get("requestId") during the request — should be a UUID
        assertThat(body)
                .isNotBlank()
                .isNotEqualTo("null")
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void mdc_isClearedAfterRequestCompletes() throws Exception {
        mockMvc.perform(get("/test/mdc-request-id"))
                .andExpect(status().isOk());

        // MdcFilter calls MDC.clear() in finally — the requestId must be gone
        assertThat(MDC.get("requestId")).isNull();
    }
}
