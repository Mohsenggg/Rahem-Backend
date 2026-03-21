package com.mgh.backend.tree.controller;

import com.mgh.backend.tree.domain.dto.InvitationCodeGenerateRequestDto;
import com.mgh.backend.tree.domain.dto.InvitationCodeResponseDto;
import com.mgh.backend.tree.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<InvitationCodeResponseDto> generateInvitation(@RequestBody @Valid InvitationCodeGenerateRequestDto request) {
        InvitationCodeResponseDto response = invitationService.generateInvitationCode(request);
        return ResponseEntity.ok(response);
    }
}

