package com.mgh.backend.tree.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgh.backend.auth.security.adapter.UserAuthAdapter;
import com.mgh.backend.tree.domain.dto.TreeHistoryDto;
import com.mgh.backend.tree.domain.entity.TreeHistory;
import com.mgh.backend.tree.domain.enums.AuditAction;
import com.mgh.backend.tree.domain.enums.AuditEntityType;
import com.mgh.backend.tree.domain.snapshot.NodeSnapshot;
import com.mgh.backend.tree.domain.snapshot.PartnerSnapshot;
import com.mgh.backend.tree.repository.TreeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TreeAuditService {

    private static final Logger log = LoggerFactory.getLogger(TreeAuditService.class);

    private final TreeHistoryRepository treeHistoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordNodeCreate(NodeSnapshot after, Long treeId) {
        if (after == null) return;
        TreeHistory history = TreeHistory.builder()
                .treeId(treeId)
                .action(AuditAction.CREATE)
                .entityType(AuditEntityType.NODE)
                .entityId(after.nodeId())
                .performedBy(currentUserId())
                .previousState(null)
                .newState(toJson(after))
                .build();

        treeHistoryRepository.save(history);
        log.debug("Audited NODE CREATE: nodeId={}, treeId={}", after.nodeId(), treeId);
    }

    @Transactional
    public void recordNodeUpdate(NodeSnapshot before, NodeSnapshot after, Long treeId) {
        if (after == null) return;
        TreeHistory history = TreeHistory.builder()
                .treeId(treeId)
                .action(AuditAction.UPDATE)
                .entityType(AuditEntityType.NODE)
                .entityId(after.nodeId())
                .performedBy(currentUserId())
                .previousState(toJson(before))
                .newState(toJson(after))
                .build();

        treeHistoryRepository.save(history);
        log.debug("Audited NODE UPDATE: nodeId={}, treeId={}", after.nodeId(), treeId);
    }

    @Transactional
    public void recordPartnerCreate(PartnerSnapshot after, Long treeId) {
        if (after == null) return;
        TreeHistory history = TreeHistory.builder()
                .treeId(treeId)
                .action(AuditAction.CREATE)
                .entityType(AuditEntityType.PARTNER)
                .entityId(after.partnershipId())
                .performedBy(currentUserId())
                .previousState(null)
                .newState(toJson(after))
                .build();

        treeHistoryRepository.save(history);
        log.debug("Audited PARTNER CREATE: partnershipId={}, treeId={}", after.partnershipId(), treeId);
    }

    @Transactional
    public void recordPartnerUpdate(PartnerSnapshot before, PartnerSnapshot after, Long treeId) {
        if (after == null) return;
        TreeHistory history = TreeHistory.builder()
                .treeId(treeId)
                .action(AuditAction.UPDATE)
                .entityType(AuditEntityType.PARTNER)
                .entityId(after.partnershipId())
                .performedBy(currentUserId())
                .previousState(toJson(before))
                .newState(toJson(after))
                .build();

        treeHistoryRepository.save(history);
        log.debug("Audited PARTNER UPDATE: partnershipId={}, treeId={}", after.partnershipId(), treeId);
    }

    @Transactional
    public void recordPartnerDelete(PartnerSnapshot before, Long treeId) {
        if (before == null) return;
        TreeHistory history = TreeHistory.builder()
                .treeId(treeId)
                .action(AuditAction.DELETE)
                .entityType(AuditEntityType.PARTNER)
                .entityId(before.partnershipId())
                .performedBy(currentUserId())
                .previousState(toJson(before))
                .newState(null)
                .build();

        treeHistoryRepository.save(history);
        log.debug("Audited PARTNER DELETE: partnershipId={}, treeId={}", before.partnershipId(), treeId);
    }

    @Transactional(readOnly = true)
    public Page<TreeHistoryDto> getTreeHistory(
            Long treeId,
            AuditAction action,
            AuditEntityType entityType,
            Long entityId,
            Pageable pageable
    ) {
        return treeHistoryRepository.findHistoryWithFilters(treeId, action, entityType, entityId, pageable)
                .map(this::toDto);
    }

    public TreeHistoryDto toDto(TreeHistory history) {
        return TreeHistoryDto.builder()
                .id(history.getId())
                .treeId(history.getTreeId())
                .action(history.getAction())
                .entityType(history.getEntityType())
                .entityId(history.getEntityId())
                .performedBy(history.getPerformedBy())
                .performedAt(history.getPerformedAt())
                .previousState(fromJson(history.getPreviousState()))
                .newState(fromJson(history.getNewState()))
                .build();
    }

    private String toJson(Object object) {
        if (object == null) return null;
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize snapshot to JSON", e);
        }
    }

    private Object fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON state in tree history: {}", json, e);
            return json;
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserAuthAdapter adapter) {
            return adapter.getUserAuth().getId();
        }
        return null;
    }
}
