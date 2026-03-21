package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.TreeNodeStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationInitiateResponseDto {

    private String firstName;
    private String familyName;
    private TreeNodeStatus status;
    private String message;
}

