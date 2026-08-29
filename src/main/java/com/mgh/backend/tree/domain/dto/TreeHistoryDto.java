package com.mgh.backend.tree.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.mgh.backend.tree.domain.enums.AuditAction;
import com.mgh.backend.tree.domain.enums.AuditEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeHistoryDto {

    private Long id;
    private Long treeId;
    private AuditAction action;
    private AuditEntityType entityType;
    private Long entityId;
    private Long performedBy;
    private LocalDateTime performedAt;
    private JsonNode previousState;
    private JsonNode newState;
}
