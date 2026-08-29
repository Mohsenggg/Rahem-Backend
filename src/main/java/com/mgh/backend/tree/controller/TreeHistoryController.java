package com.mgh.backend.tree.controller;

import com.mgh.backend.tree.domain.dto.TreeHistoryDto;
import com.mgh.backend.tree.domain.enums.AuditAction;
import com.mgh.backend.tree.domain.enums.AuditEntityType;
import com.mgh.backend.tree.service.TreeAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trees/{treeId}/history")
@RequiredArgsConstructor
public class TreeHistoryController {

    private final TreeAuditService treeAuditService;

    @GetMapping
    public ResponseEntity<Page<TreeHistoryDto>> getTreeHistory(
            @PathVariable Long treeId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) Long entityId,
            @PageableDefault(size = 20, sort = "performedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                treeAuditService.getTreeHistory(treeId, action, entityType, entityId, pageable)
        );
    }
}
