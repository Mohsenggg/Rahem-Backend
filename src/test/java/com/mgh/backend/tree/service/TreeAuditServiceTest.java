package com.mgh.backend.tree.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgh.backend.tree.domain.dto.TreeHistoryDto;
import com.mgh.backend.tree.domain.entity.TreeHistory;
import com.mgh.backend.tree.domain.enums.AuditAction;
import com.mgh.backend.tree.domain.enums.AuditEntityType;
import com.mgh.backend.tree.domain.snapshot.NodeSnapshot;
import com.mgh.backend.tree.domain.snapshot.PartnerSnapshot;
import com.mgh.backend.tree.repository.TreeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreeAuditServiceTest {

    @Mock
    private TreeHistoryRepository treeHistoryRepository;

    private ObjectMapper objectMapper;
    private TreeAuditService treeAuditService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        treeAuditService = new TreeAuditService(treeHistoryRepository, objectMapper);
    }

    @Test
    @DisplayName("recordNodeCreate saves TreeHistory with action CREATE, entityType NODE and newState")
    void recordNodeCreate_success() {
        NodeSnapshot after = new NodeSnapshot(
                62L, "Jack", 10L, null, "MALE", true, 2L, false, 0L
        );

        treeAuditService.recordNodeCreate(after, 1L);

        ArgumentCaptor<TreeHistory> captor = ArgumentCaptor.forClass(TreeHistory.class);
        verify(treeHistoryRepository).save(captor.capture());

        TreeHistory saved = captor.getValue();

        assertThat(saved.getTreeId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.NODE);
        assertThat(saved.getEntityId()).isEqualTo(62L);
        assertThat(saved.getPreviousState()).isNull();

        JsonNode newState = saved.getNewState();

        assertThat(newState.get("nodeId").asLong()).isEqualTo(62L);
        assertThat(newState.get("nodeName").asText()).isEqualTo("Jack");
        assertThat(newState.get("isExternal").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("recordNodeUpdate saves TreeHistory with action UPDATE and both states")
    void recordNodeUpdate_success() {
        NodeSnapshot before = new NodeSnapshot(
                62L, "Jac", 10L, null, "MALE", true, 2L, false, 0L
        );

        NodeSnapshot after = new NodeSnapshot(
                62L, "Jack", 10L, null, "MALE", true, 2L, false, 1L
        );

        treeAuditService.recordNodeUpdate(before, after, 1L);

        ArgumentCaptor<TreeHistory> captor = ArgumentCaptor.forClass(TreeHistory.class);
        verify(treeHistoryRepository).save(captor.capture());

        TreeHistory saved = captor.getValue();

        assertThat(saved.getTreeId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.NODE);
        assertThat(saved.getEntityId()).isEqualTo(62L);

        JsonNode previousState = saved.getPreviousState();
        JsonNode newState = saved.getNewState();

        assertThat(previousState.get("nodeName").asText()).isEqualTo("Jac");
        assertThat(newState.get("nodeName").asText()).isEqualTo("Jack");

        assertThat(previousState.get("version").asLong()).isEqualTo(0L);
        assertThat(newState.get("version").asLong()).isEqualTo(1L);
    }

    @Test
    @DisplayName("recordPartnerCreate saves TreeHistory with action CREATE and entityType PARTNER")
    void recordPartnerCreate_success() {
        PartnerSnapshot after = new PartnerSnapshot(
                100L, 10L, 20L, "ACTIVE", true, null, null
        );

        treeAuditService.recordPartnerCreate(after, 1L);

        ArgumentCaptor<TreeHistory> captor = ArgumentCaptor.forClass(TreeHistory.class);
        verify(treeHistoryRepository).save(captor.capture());

        TreeHistory saved = captor.getValue();

        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.PARTNER);
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getPreviousState()).isNull();

        JsonNode newState = saved.getNewState();

        assertThat(newState.get("partnershipId").asLong()).isEqualTo(100L);
        assertThat(newState.get("nodeId").asLong()).isEqualTo(10L);
        assertThat(newState.get("partnerId").asLong()).isEqualTo(20L);
    }

    @Test
    @DisplayName("recordPartnerDelete saves TreeHistory with action DELETE and null newState")
    void recordPartnerDelete_success() {
        PartnerSnapshot before = new PartnerSnapshot(
                100L, 10L, 20L, "ACTIVE", true, null, null
        );

        treeAuditService.recordPartnerDelete(before, 1L);

        ArgumentCaptor<TreeHistory> captor = ArgumentCaptor.forClass(TreeHistory.class);
        verify(treeHistoryRepository).save(captor.capture());

        TreeHistory saved = captor.getValue();

        assertThat(saved.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.PARTNER);
        assertThat(saved.getEntityId()).isEqualTo(100L);

        JsonNode previousState = saved.getPreviousState();

        assertThat(previousState.get("partnershipId").asLong()).isEqualTo(100L);
        assertThat(saved.getNewState()).isNull();
    }

    @Test
    @DisplayName("getTreeHistory returns paginated TreeHistoryDto with JSON states")
    void getTreeHistory_success() {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode previousState = objectMapper.createObjectNode()
                .put("nodeId", 62L)
                .put("nodeName", "Jac");

        JsonNode newState = objectMapper.createObjectNode()
                .put("nodeId", 62L)
                .put("nodeName", "Jack");

        TreeHistory history = TreeHistory.builder()
                .id(1L)
                .treeId(1L)
                .action(AuditAction.UPDATE)
                .entityType(AuditEntityType.NODE)
                .entityId(62L)
                .performedBy(5L)
                .performedAt(LocalDateTime.now())
                .previousState(previousState)
                .newState(newState)
                .build();

        Page<TreeHistory> page = new PageImpl<>(List.of(history));

        when(treeHistoryRepository.findHistoryWithFilters(
                eq(1L), any(), any(), any(), any()
        )).thenReturn(page);

        Page<TreeHistoryDto> result = treeAuditService.getTreeHistory(
                1L, null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);

        TreeHistoryDto dto = result.getContent().get(0);

        assertThat(dto.getEntityId()).isEqualTo(62L);

        assertThat(dto.getPreviousState()).isNotNull();
        assertThat(dto.getPreviousState().get("nodeName").asText())
                .isEqualTo("Jac");

        assertThat(dto.getNewState()).isNotNull();
        assertThat(dto.getNewState().get("nodeName").asText())
                .isEqualTo("Jack");
    }}


