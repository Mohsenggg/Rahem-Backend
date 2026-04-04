package com.mgh.backend.invitation.controller;

import com.mgh.backend.auth.security.SecurityUtils;
import com.mgh.backend.invitation.domain.dto.InvitationAnswerRequestDto;
import com.mgh.backend.invitation.domain.dto.InvitationAnswerResponseDto;
import com.mgh.backend.invitation.domain.dto.InvitationQuestionResponseDto;
import com.mgh.backend.invitation.service.InvitationQuizService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
public class InvitationQuizController {

    private final InvitationQuizService invitationQuizService;

    public InvitationQuizController(InvitationQuizService invitationQuizService) {
        this.invitationQuizService = invitationQuizService;
    }

    @GetMapping("/question")
    public ResponseEntity<InvitationQuestionResponseDto> getQuestion(Authentication authentication) {
        long userId = SecurityUtils.requireUserId(authentication);
        return ResponseEntity.ok(invitationQuizService.getQuestion(userId));
    }

    @PostMapping("/answer")
    public ResponseEntity<InvitationAnswerResponseDto> submitAnswer(
            Authentication authentication,
            @RequestBody @Valid InvitationAnswerRequestDto request) {
        long userId = SecurityUtils.requireUserId(authentication);
        return ResponseEntity.ok(invitationQuizService.submitAnswer(userId, request.getQuestionId(), request.getAnswer()));
    }
}
