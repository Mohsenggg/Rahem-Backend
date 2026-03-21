package com.mgh.backend.tree.controller;

import com.mgh.backend.tree.domain.dto.RegistrationInitiateRequestDto;
import com.mgh.backend.tree.domain.dto.RegistrationInitiateResponseDto;
import com.mgh.backend.tree.domain.dto.RegistrationSubmitRequestDto;
import com.mgh.backend.tree.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<RegistrationInitiateResponseDto> initiate(@RequestBody @Valid RegistrationInitiateRequestDto request) {
        RegistrationInitiateResponseDto response = registrationService.initiateRegistration(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit")
    public ResponseEntity<Void> submit(@RequestBody @Valid RegistrationSubmitRequestDto request) {
        registrationService.submitRegistration(request);
        return ResponseEntity.ok().build();
    }
}

