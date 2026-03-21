package com.mgh.backend.tree.controller;

import com.mgh.backend.tree.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registration")
public class AdminRegistrationController {

    private final RegistrationService registrationService;

    public AdminRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable("id") Long id, Authentication authentication) {
        String approvedBy = authentication != null ? authentication.getName() : "system";
        registrationService.approveRegistration(id, approvedBy);
        return ResponseEntity.ok().build();
    }
}

