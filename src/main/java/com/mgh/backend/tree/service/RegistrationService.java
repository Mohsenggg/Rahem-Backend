package com.mgh.backend.tree.service;

import com.mgh.backend.auth.domain.entity.UserAuth;
import com.mgh.backend.auth.domain.entity.UserProfile;
import com.mgh.backend.auth.domain.enums.Role;
import com.mgh.backend.auth.repository.UserAuthRepo;
import com.mgh.backend.auth.repository.UserProfileRepository;
import com.mgh.backend.tree.domain.dto.RegistrationInitiateRequestDto;
import com.mgh.backend.tree.domain.dto.RegistrationInitiateResponseDto;
import com.mgh.backend.tree.domain.dto.RegistrationSubmitRequestDto;
import com.mgh.backend.tree.domain.entity.RegisterForm;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.domain.enums.RegisterStatus;
import com.mgh.backend.tree.domain.enums.TreeNodeStatus;
import com.mgh.backend.tree.repository.RegisterFormRepository;
import com.mgh.backend.tree.repository.NodeRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistrationService {

    private final InvitationService invitationService;
    private final NodeRepo nodeRepo;
    private final RegisterFormRepository registerFormRepository;
    private final UserAuthRepo userAuthRepo;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(InvitationService invitationService,
                               NodeRepo nodeRepo,
                               RegisterFormRepository registerFormRepository,
                               UserAuthRepo userAuthRepo,
                               UserProfileRepository userProfileRepository,
                               PasswordEncoder passwordEncoder) {
        this.invitationService = invitationService;
        this.nodeRepo = nodeRepo;
        this.registerFormRepository = registerFormRepository;
        this.userAuthRepo = userAuthRepo;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public RegistrationInitiateResponseDto initiateRegistration(RegistrationInitiateRequestDto request) {
        Long nodeId = invitationService.validateAndExtractNodeId(request.getInvitationCode());

        Node node = nodeRepo.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        if (!request.getInvitationCode().equals(node.getInvitationCode())) {
            throw new IllegalArgumentException("Invitation code does not match this node");
        }

        TreeNodeStatus status = node.getStatus();
        if (status == TreeNodeStatus.INACTIVE) {
            return RegistrationInitiateResponseDto.builder()
                    .firstName(node.getNodeName())
                    .familyName(node.getNodeParentName())
                    .status(status)
                    .build();
        } else if (status == TreeNodeStatus.PENDING) {
            return RegistrationInitiateResponseDto.builder()
                    .status(status)
                    .message("Your registration is waiting for approval.")
                    .build();
        } else {
            return RegistrationInitiateResponseDto.builder()
                    .status(status)
                    .message("Your profile is already activated. Please login.")
                    .build();
        }
    }

    @Transactional
    public void submitRegistration(RegistrationSubmitRequestDto request) {
        Long nodeId = invitationService.validateAndExtractNodeId(request.getInvitationCode());

        Node node = nodeRepo.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        if (!request.getInvitationCode().equals(node.getInvitationCode())) {
            throw new IllegalArgumentException("Invitation code does not match this node");
        }

        if (node.getStatus() == TreeNodeStatus.ACTIVATED) {
            throw new IllegalStateException("Profile already activated");
        }

        String username = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername()
                : request.getPhoneNumber();

        String encodedPassword = passwordEncoder.encode(request.getPassword()); // phone used as initial password placeholder


        RegisterForm registerForm = RegisterForm.builder()
                .nodeId(node.getId())
                .username(username)
                .email(request.getEmail())
                .phone(request.getPhoneNumber())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .address(request.getAddress())
                .password(encodedPassword)
                .status(RegisterStatus.SUBMITTED)
                .build();

        registerFormRepository.save(registerForm);

        node.setStatus(TreeNodeStatus.PENDING);
        nodeRepo.save(node);
    }

    @Transactional
    public void approveRegistration(Long registerFormId, String approvedBy) {
        RegisterForm registerForm = registerFormRepository.findById(registerFormId)
                .orElseThrow(() -> new EntityNotFoundException("Register form not found"));

        if (registerForm.getStatus() == RegisterStatus.APPROVED) {
            return;
        }

        Node node = nodeRepo.findById(registerForm.getNodeId())
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));


        UserAuth userAuth = UserAuth.builder()
                .username(registerForm.getUsername())
                .email(registerForm.getEmail())
                .password(registerForm.getPassword())
                .phone(registerForm.getPhone())
                .fullName(node.getNodeName() + " " + node.getNodeParentName())
                .role(Role.USER)
                .enabled(true)
                .locked(false)
                .build();

        userAuthRepo.save(userAuth);

        UserProfile userProfile = UserProfile.builder()
                .userAuth(userAuth)
                .birthDate(registerForm.getBirthDate())
                .gender(registerForm.getGender())
                .address(registerForm.getAddress())
                .build();

        userProfileRepository.save(userProfile);

        node.setUserId(userAuth.getId());
        node.setStatus(TreeNodeStatus.ACTIVATED);
        nodeRepo.save(node);

        registerForm.setStatus(RegisterStatus.APPROVED);
        registerForm.setApprovedAt(LocalDateTime.now());
        registerForm.setApprovedBy(approvedBy);
        registerFormRepository.save(registerForm);
    }
}

