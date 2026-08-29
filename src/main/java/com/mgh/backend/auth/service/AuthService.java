package com.mgh.backend.auth.service;

import com.mgh.backend.auth.domain.dto.*;
import com.mgh.backend.auth.domain.entity.UserAuth;
import com.mgh.backend.auth.domain.enums.Role;
import com.mgh.backend.auth.repository.UserAuthRepo;
import com.mgh.backend.auth.security.service.JwtService;
import com.mgh.backend.auth.security.adapter.UserAuthAdapter;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.password.PasswordEncoder;


import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAuthRepo userAuthRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public ResponseEntity<Map<String, String>> register(RegisterRequestDto registerRequestDto) {

        // Create new userAuth
        UserAuth userAuth = UserAuth.builder()
                .username(registerRequestDto.getUsername())
                .email(registerRequestDto.getEmail())
                .fullName(registerRequestDto.getFullName())
//                .lastName(request.getLastName())
                .password(passwordEncoder.encode(registerRequestDto.getPassword()))
                .enabled(true)
                .locked(false)
                .role(Role.USER)
                .build();

        userAuthRepo.save(userAuth);

        return ResponseEntity.ok(Collections.singletonMap("message", "User registered successfully"));

    }

    // ===================================================================
    // ===================================================================

    private Authentication getAuthentication(AuthRequestDto request) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
    }

    public AuthResponseDto login(AuthRequestDto request) {

        // 1️⃣ Authenticate (delegates to AuthenticationManager, throws AuthenticationException on bad credentials/status)
        Authentication authentication = getAuthentication(request);

        // 2️⃣ Authentication succeeded → get principal
        UserAuthAdapter userAuthAdapter = (UserAuthAdapter) authentication.getPrincipal();
        UserAuth userAuth = userAuthAdapter.getUserAuth();

        // 3️⃣ Generate JWT
        TokenExpiryDto tokenWithExpiry = jwtService.generateToken(userAuthAdapter);

        return AuthResponseDto.builder()
                .token(tokenWithExpiry.getToken())
                .user(userToUserDto(userAuth))
                .expiresIn(tokenWithExpiry.getExpiry())
                .build();
    }

    // ===================================================================
    // ===================================================================

    private UserDataDto userToUserDto(UserAuth userAuth) {
        return UserDataDto.builder()
                .id(userAuth.getId())
                .username(userAuth.getUsername())
                .email(userAuth.getEmail())
                .roles(Collections.singleton(userAuth.getRole()))
                .build();
    }

}
