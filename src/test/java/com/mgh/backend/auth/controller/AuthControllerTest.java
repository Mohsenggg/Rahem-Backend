package com.mgh.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgh.backend.auth.domain.dto.AuthRequestDto;
import com.mgh.backend.auth.domain.dto.AuthResponseDto;
import com.mgh.backend.auth.domain.dto.RegisterRequestDto;
import com.mgh.backend.auth.domain.dto.UserDataDto;
import com.mgh.backend.auth.domain.enums.Role;
import com.mgh.backend.auth.service.AuthService;
import com.mgh.backend.auth.service.RegistrationService;
import com.mgh.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials returns 200 OK")
    void login_success() throws Exception {
        AuthRequestDto request = new AuthRequestDto("validuser", "password123", false);
        AuthResponseDto response = AuthResponseDto.builder()
                .token("jwt.test.token")
                .user(UserDataDto.builder().id(1L).username("validuser").roles(Set.of(Role.USER)).build())
                .expiresIn(Instant.now().plusSeconds(3600))
                .build();

        when(authService.login(any(AuthRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.test.token"))
                .andExpect(jsonPath("$.user.username").value("validuser"));
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong credentials returns 401 UNAUTHORIZED rather than 500")
    void login_wrongCredentials_returns401() throws Exception {
        AuthRequestDto request = new AuthRequestDto("wronguser", "wrongpassword", false);

        when(authService.login(any(AuthRequestDto.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    @DisplayName("POST /api/auth/login with disabled user returns 403 FORBIDDEN")
    void login_disabledUser_returns403() throws Exception {
        AuthRequestDto request = new AuthRequestDto("disableduser", "password123", false);

        when(authService.login(any(AuthRequestDto.class)))
                .thenThrow(new DisabledException("User account is disabled"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("User account is disabled"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    @DisplayName("POST /api/auth/register returns 200 OK with AuthResponseDto containing token")
    void register_success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setFullName("New User");

        AuthResponseDto response = AuthResponseDto.builder()
                .token("jwt.register.token")
                .user(UserDataDto.builder().id(2L).username("newuser").roles(Set.of(Role.USER)).build())
                .expiresIn(Instant.now().plusSeconds(3600))
                .build();

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.register.token"))
                .andExpect(jsonPath("$.user.username").value("newuser"));
    }
}
