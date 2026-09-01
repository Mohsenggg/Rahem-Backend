package com.mgh.backend.auth.domain.dto.register;

import com.mgh.backend.tree.domain.enums.TreeNodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationInitiateResponseDto {

    private Long nodeId;
    private String firstName;
    private String parentName;
    private String gender;
    private TreeNodeStatus status;
    private String message;
}

