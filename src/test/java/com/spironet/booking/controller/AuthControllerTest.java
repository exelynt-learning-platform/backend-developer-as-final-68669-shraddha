package com.spironet.booking.controller;

import com.spironet.booking.AbstractIntegrationTest;
import com.spironet.booking.dto.auth.LoginRequest;
import com.spironet.booking.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractIntegrationTest {

    @Test
    void login_withValidAdminCredentials_returnsTokenAndRole() throws Exception {
        LoginRequest request = new LoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_withValidUserCredentials_returnsUserRole() throws Exception {
        LoginRequest request = new LoginRequest(USER1_USERNAME, USER_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest(ADMIN_USERNAME, "wrong-password");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_withUnknownUsername_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("no-such-user", "whatever");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withBlankUsername_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void register_withNewUsername_createsUserRole() throws Exception {
        RegisterRequest request = new RegisterRequest("newbie", "newbie@example.com", "Passw0rd!");

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // The newly registered user must default to USER role, never ADMIN.
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest("newbie", "Passw0rd!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_withDuplicateUsername_returnsConflict() throws Exception {
        RegisterRequest request = new RegisterRequest(ADMIN_USERNAME, "another@example.com", "Passw0rd!");

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
