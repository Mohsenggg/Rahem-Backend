package com.mgh.backend.tree.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TreeDTO {
    private Long treeId;
    private String treeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}