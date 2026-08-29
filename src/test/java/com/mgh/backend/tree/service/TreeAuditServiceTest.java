package com.mgh.backend.tree.service;

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
    @DisplayName("recordNodeCreate saves TreeHistory with action CREATE, entityType NODE and serialized newState")
    void recordNodeCreate_success() {
        NodeSnapshot after = new NodeSnapshot(62L, "Jack", 10L, null, "MALE", true, 2L, false, 0L);

        treeAuditService.recordNodeCreate(after, 1L);

        ArgumentCaptor<TreeHistory> captor = ArgumentCaptor.forClass(TreeHistory.class);
        verify(treeHistoryRepository).save(captor.capture());

        TreeHistory saved = captor.getValue();
        assertThat(saved.getTreeId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.NODE);
        assertThat(saved.getEntityId()).isEqualTo(62L);
        assertThat(saved.getPreviousState()).isNull();
        assertThat(saved.getNewState()).contains("\"nodeId\":62");
        assertThat(saved.getNewState()).contains("\"nodeName\":\"Jack\"");
        assertThat(saved.getNewState()).contains("\"isExternal\":false");
    }

    @Test
    @DisplayName("recordNodeUpdate saves TreeHistory with action UPDATE and both previousState and newState")
    void recordNodeUpdate_success() {
        NodeSnapshot before = new NodeSnapshot(62L, "Jac", 10L, null, "MALE", true, 2L, false, 0L);
        NodeSnapshot after  = new NodeSnapshot(62L, "Jack", 10L, null, "MALE", true, 2L, false, 1L);

        treeAuditService.recordNodeUpdate(before, after, 1L);

        ArgumentCaptor<TreeHistory> captor = ArgumentCaptor.forClass(TreeHistory.class);
        verify(treeHistoryRepository).save(captor.capture());

        TreeHistory saved = captor.getValue();
        assertThat(saved.getTreeId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.NODE);
        assertThat(saved.getEntityId()).isEqualTo(62L);
        assertThat(saved.getPreviousState()).contains("\"nodeName\":\"Jac\"");
        assertThat(saved.getNewState()).contains("\"nodeName\":\"Jack\"");
    }

    @Test
    @DisplayName("recordPartnerCreate saves TreeHistory with action CREATE and entityType PARTNER")
    void recordPartnerCreate_success() {
        PartnerSnapshot after = new PartnerSnapshot(100L, 10L, 20L, "ACTIVE", true, null, null);

        treeAuditService.recordPartnerCreate(after, 1L);

        ArgumentCaptor<TreeHistory> captor = ArgumentCaptor.forClass(TreeHistory.class);
        verify(treeHistoryRepository).save(captor.capture());

        TreeHistory saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.PARTNER);
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getPreviousState()).isNull();
        assertThat(saved.getNewState()).contains("\"partnershipId\":100");
        assertThat(saved.getNewState()).contains("\"nodeId\":10");
        assertThat(saved.getNewState()).contains("\"partnerId\":20");
    }

    @Test
    @DisplayName("recordPartnerDelete saves TreeHistory with action DELETE and null newState")
    void recordPartnerDelete_success() {
        PartnerSnapshot before = new PartnerSnapshot(100L, 10L, 20L, "ACTIVE", true, null, null);

        treeAuditService.recordPartnerDelete(before, 1L);

        ArgumentCaptor<TreeHistory> captor = ArgumentCaptor.forClass(TreeHistory.class);
        verify(treeHistoryRepository).save(captor.capture());

        TreeHistory saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.PARTNER);
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getPreviousState()).contains("\"partnershipId\":100");
        assertThat(saved.getNewState()).isNull();
    }

    @Test
    @DisplayName("getTreeHistory returns paginated TreeHistoryDto with parsed JSON states")
    void getTreeHistory_success() {
        TreeHistory history = TreeHistory.builder()
                .id(1L)
                .treeId(1L)
                .action(AuditAction.UPDATE)
                .entityType(AuditEntityType.NODE)
                .entityId(62L)
                .performedBy(5L)
                .performedAt(LocalDateTime.now())
                .previousState("{\"nodeId\":62,\"nodeName\":\"Jac\"}")
                .newState("{\"nodeId\":62,\"nodeName\":\"Jack\"}")
                .build();

        Page<TreeHistory> page = new PageImpl<>(List.of(history));
        when(treeHistoryRepository.findHistoryWithFilters(eq(1L), any(), any(), any(), any()))
                .thenReturn(page);

        Page<TreeHistoryDto> result = treeAuditService.getTreeHistory(
                1L, null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        TreeHistoryDto dto = result.getContent().get(0);
        assertThat(dto.getEntityId()).isEqualTo(62L);
        assertThat(dto.getPreviousState()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) dto.getPreviousState()).get("nodeName")).isEqualTo("Jac");
        assertThat(((Map<?, ?>) dto.getNewState()).get("nodeName")).isEqualTo("Jack");
    }
}
