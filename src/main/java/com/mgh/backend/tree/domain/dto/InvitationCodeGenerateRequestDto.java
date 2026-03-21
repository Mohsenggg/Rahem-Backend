package com.mgh.backend.tree.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvitationCodeGenerateRequestDto {

    @NotNull
    private Long nodeId;
}

