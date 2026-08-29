package com.mgh.backend.auth.service;

import com.mgh.backend.auth.domain.dto.AuthRequestDto;
import com.mgh.backend.auth.domain.dto.AuthResponseDto;
import com.mgh.backend.auth.domain.dto.TokenExpiryDto;
import com.mgh.backend.auth.domain.entity.UserAuth;
import com.mgh.backend.auth.domain.enums.Role;
import com.mgh.backend.auth.repository.UserAuthRepo;
import com.mgh.backend.auth.security.adapter.UserAuthAdapter;
import com.mgh.backend.auth.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAuthRepo userAuthRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UserAuth testUser;
    private UserAuthAdapter testUserAdapter;

    @BeforeEach
    void setUp() {
        testUser = UserAuth.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password("encoded_pass")
                .role(Role.USER)
                .enabled(true)
                .locked(false)
                .build();
        testUserAdapter = new UserAuthAdapter(testUser);
    }

    @Test
    @DisplayName("login succeeds with valid credentials and returns AuthResponseDto with token")
    void login_success() {
        AuthRequestDto request = new AuthRequestDto("testuser", "password123", false);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUserAdapter);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        Instant expiry = Instant.now().plusSeconds(3600);
        when(jwtService.generateToken(testUserAdapter))
                .thenReturn(new TokenExpiryDto("jwt.mock.token", expiry));

        AuthResponseDto response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.mock.token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        assertThat(response.getExpiresIn()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("login throws BadCredentialsException when credentials are invalid")
    void login_badCredentials() {
        AuthRequestDto request = new AuthRequestDto("testuser", "wrongpassword", false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");
    }

    @Test
    @DisplayName("login throws DisabledException when user account is disabled")
    void login_disabledAccount() {
        AuthRequestDto request = new AuthRequestDto("disableduser", "password123", false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User account is disabled"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(DisabledException.class)
                .hasMessage("User account is disabled");
    }
}
