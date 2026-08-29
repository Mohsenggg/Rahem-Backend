package com.mgh.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
    }

    @Test
    @DisplayName("handleBadCredentials returns 401 UNAUTHORIZED for BadCredentialsException")
    void handleBadCredentials_badCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ApiError> response = exceptionHandler.handleBadCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
        assertThat(response.getBody().getPath()).isEqualTo("/api/auth/login");
    }

    @Test
    @DisplayName("handleBadCredentials returns 401 UNAUTHORIZED for UsernameNotFoundException")
    void handleBadCredentials_usernameNotFound() {
        UsernameNotFoundException ex = new UsernameNotFoundException("User not found");
        ResponseEntity<ApiError> response = exceptionHandler.handleBadCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
        assertThat(response.getBody().getPath()).isEqualTo("/api/auth/login");
    }

    @Test
    @DisplayName("handleDisabled returns 403 FORBIDDEN for DisabledException")
    void handleDisabled() {
        DisabledException ex = new DisabledException("User account is disabled");
        ResponseEntity<ApiError> response = exceptionHandler.handleDisabled(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("User account is disabled");
    }

    @Test
    @DisplayName("handleLocked returns 403 FORBIDDEN for LockedException")
    void handleLocked() {
        LockedException ex = new LockedException("User account is locked");
        ResponseEntity<ApiError> response = exceptionHandler.handleLocked(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("User account is locked");
    }

    @Test
    @DisplayName("handleAccessDenied returns 403 FORBIDDEN for AccessDeniedException")
    void handleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<ApiError> response = exceptionHandler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }
}
