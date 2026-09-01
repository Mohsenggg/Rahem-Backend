package com.mgh.backend.auth.service;

import com.mgh.backend.auth.domain.dto.*;
import com.mgh.backend.auth.domain.entity.UserAuth;
import com.mgh.backend.auth.domain.enums.Role;
import com.mgh.backend.auth.repository.UserAuthRepo;
import com.mgh.backend.auth.security.service.JwtService;
import com.mgh.backend.auth.security.adapter.UserAuthAdapter;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.repository.NodeRepo;
import lombok.RequiredArgsConstructor;
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
    private final NodeRepo nodeRepo;
    private final RegistrationService registrationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponseDto register(RegisterRequestDto registerRequestDto) {
        String fullName = registerRequestDto.getFullName();
        Node node = null;

        if (registerRequestDto.getInvitationCode() != null && !registerRequestDto.getInvitationCode().isBlank()) {
            try {
                Long nodeId = registrationService.validateAndExtractNodeId(registerRequestDto.getInvitationCode());
                node = nodeRepo.findByNodeIdAndIsDeletedFalse(nodeId).orElse(null);
                if (node != null) {
                    String parentName = "";
                    if (node.getFatherId() != null) {
                        parentName = nodeRepo.findByNodeIdAndIsDeletedFalse(node.getFatherId()).map(Node::getNodeName).orElse("");
                    } else if (node.getMotherId() != null) {
                        parentName = nodeRepo.findByNodeIdAndIsDeletedFalse(node.getMotherId()).map(Node::getNodeName).orElse("");
                    }
                    if (fullName == null || fullName.isBlank()) {
                        fullName = (node.getNodeName() + (parentName.isBlank() ? "" : " " + parentName)).trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (fullName == null || fullName.isBlank()) {
            fullName = registerRequestDto.getUsername();
        }

        // Create new userAuth
        UserAuth userAuth = UserAuth.builder()
                .username(registerRequestDto.getUsername())
                .email(registerRequestDto.getEmail())
                .fullName(fullName)
                .phone(registerRequestDto.getPhone())
                .password(passwordEncoder.encode(registerRequestDto.getPassword()))
                .enabled(true)
                .locked(false)
                .role(Role.USER)
                .build();

        userAuth = userAuthRepo.save(userAuth);

        if (node != null) {
            node.setUserId(userAuth.getId());
            node.setStatus(com.mgh.backend.tree.domain.enums.TreeNodeStatus.ACTIVATED);
            nodeRepo.save(node);
        }

        UserAuthAdapter userAuthAdapter = new UserAuthAdapter(userAuth);
        TokenExpiryDto tokenWithExpiry = jwtService.generateToken(userAuthAdapter);

        return AuthResponseDto.builder()
                .token(tokenWithExpiry.getToken())
                .user(userToUserDto(userAuth))
                .expiresIn(tokenWithExpiry.getExpiry())
                .build();
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
        String nodeName = null;
        if (userAuth.getId() != null) {
            nodeName = nodeRepo.findFirstByUserIdAndIsDeletedFalse(userAuth.getId())
                    .map(Node::getNodeName)
                    .orElse(null);
        }
        if (nodeName == null || nodeName.isBlank()) {
            nodeName = userAuth.getFullName();
        }
        if (nodeName == null || nodeName.isBlank()) {
            nodeName = userAuth.getUsername();
        }

        String fullName = userAuth.getFullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = nodeName;
        }

        return UserDataDto.builder()
                .id(userAuth.getId())
                .username(userAuth.getUsername())
                .email(userAuth.getEmail())
                .fullName(fullName)
                .nodeName(nodeName)
                .roles(Collections.singleton(userAuth.getRole()))
                .build();
    }

}
