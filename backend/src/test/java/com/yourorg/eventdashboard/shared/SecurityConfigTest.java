package com.yourorg.eventdashboard.shared;

import com.yourorg.eventdashboard.admin.AdminUser;
import com.yourorg.eventdashboard.admin.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mock"})
class SecurityConfigTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @MockBean AdminUserRepository adminUserRepository;

    @BeforeEach
    void setUp() {
        AdminUser mockAdmin = org.mockito.Mockito.mock(AdminUser.class);
        when(mockAdmin.getUsername()).thenReturn("admin");
        when(mockAdmin.getPasswordHash()).thenReturn(passwordEncoder.encode("password"));
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(mockAdmin));
    }

    @Test
    void publicEventEndpoint_isNotBlockedBySecurity() throws Exception {
        // EventController is not yet implemented; verify security does NOT return 401/403.
        // Any non-401/403 response proves the request passed Spring Security's permitAll() rule.
        mockMvc.perform(get("/api/events"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void publicCategoriesEndpoint_isNotBlockedBySecurity() throws Exception {
        mockMvc.perform(get("/api/events/categories"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void subscriptionEndpoint_isNotBlockedBySecurity() throws Exception {
        // POST /api/subscriptions is permitAll — security passes, business logic may reject for other reasons.
        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"00000000-0000-0000-0000-000000000001\",\"phoneNumber\":\"+14155552671\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void adminEndpoint_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_returns401_withMalformedToken() throws Exception {
        mockMvc.perform(get("/api/admin/events")
                        .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_returns401_withExpiredToken() throws Exception {
        JwtService expiredService = new JwtService(
                "test-secret-key-for-testing-only-minimum-256-bits", -1L);
        String expiredToken = expiredService.generateToken("admin");

        mockMvc.perform(get("/api/admin/events")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_isAccessible_withValidAdminJwt() throws Exception {
        String token = jwtService.generateToken("admin");

        // Returns 404 because AdminController is not yet implemented in this story;
        // the important assertion is that Spring Security passed the request (not 401/403).
        mockMvc.perform(get("/api/admin/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
