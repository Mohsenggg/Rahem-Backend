package com.mgh.backend.invitation.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvitationAnswerRequestDto {

    @NotNull
    private Long questionId;

    @NotBlank
    private String answer;
}
