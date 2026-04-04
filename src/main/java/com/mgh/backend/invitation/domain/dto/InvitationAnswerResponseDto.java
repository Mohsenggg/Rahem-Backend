package com.mgh.backend.invitation.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationAnswerResponseDto {

    private boolean correct;
    private String nextStep;
}
