package com.monday.app.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.auth.dto.LoginRequest;
import com.monday.app.auth.dto.RegisterRequest;
import com.monday.app.auth.dto.UserResponse;
import com.monday.app.auth.service.AuthService;
import com.monday.app.security.MondayUserDetails;
import com.monday.app.shared.exception.BusinessRuleException;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testRegister_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(userId, "testuser", "test@monday.io", "ROLE_USER");
        when(authService.register(any(RegisterRequest.class), any(), any())).thenReturn(response);

        RegisterRequest request = new RegisterRequest("testuser", "test@monday.io", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@monday.io"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void testRegister_ValidationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(userId, "elena.rostova", "elena@sovereign.io", "ROLE_USER");
        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(response);

        LoginRequest request = new LoginRequest("elena.rostova", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("elena.rostova"))
                .andExpect(jsonPath("$.email").value("elena@sovereign.io"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class), any(), any()))
                .thenThrow(new BusinessRuleException("INVALID_CREDENTIALS", "Invalid username or password"));

        LoginRequest request = new LoginRequest("elena.rostova", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void testLogout_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout(any(), any());
    }

    @Test
    void testGetCurrentUser_Authenticated() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(userId, "alexander.cross", "alex@sovereign.io", "ROLE_ADMIN");
        when(authService.getCurrentUser(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alexander.cross"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void testGetCurrentUser_Unauthenticated() throws Exception {
        when(authService.getCurrentUser(any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
