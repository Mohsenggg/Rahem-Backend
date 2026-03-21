package com.mgh.backend.tree.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrationInitiateRequestDto {

    @NotBlank
    private String invitationCode;
}

