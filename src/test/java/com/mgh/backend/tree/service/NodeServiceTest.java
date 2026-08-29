package com.mgh.backend.tree.service;

import com.mgh.backend.tree.domain.dto.CreateNodeRequestDto;
import com.mgh.backend.tree.domain.dto.NodeResponseDto;
import com.mgh.backend.tree.domain.dto.UpdateNodeRequestDto;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.domain.entity.Tree;
import com.mgh.backend.tree.domain.enums.Gender;
import com.mgh.backend.tree.domain.snapshot.NodeSnapshot;
import com.mgh.backend.tree.mapper.NodeMapper;
import com.mgh.backend.tree.repository.NodePartnerRepository;
import com.mgh.backend.tree.repository.NodeRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NodeServiceTest {

    @Mock
    private NodeRepo nodeRepo;

    @Mock
    private NodeMapper nodeMapper;

    @Mock
    private NodePartnerRepository nodePartnerRepository;

    @Mock
    private NodePartnerService nodePartnerService;

    @Mock
    private TreeAuditService treeAuditService;

    @InjectMocks
    private NodeService nodeService;

    private Tree tree;
    private Node parentNode;

    @BeforeEach
    void setUp() {
        tree = new Tree();
        tree.setTreeId(1L);

        parentNode = new Node();
        parentNode.setId(10L);
        parentNode.setNodeId(10L);
        parentNode.setNodeName("Father");
        parentNode.setGender(Gender.MALE);
        parentNode.setLevel(1L);
        parentNode.setTree(tree);
        parentNode.setIsAlive(true);
        parentNode.setIsDeleted(false);
        parentNode.setIsExternal(false);
        parentNode.setVersion(0L);

        when(nodeMapper.toResponse(any(Node.class))).thenAnswer(inv -> {
            Node n = inv.getArgument(0);
            return NodeResponseDto.builder()
                    .id(n.getId())
                    .nodeId(n.getNodeId())
                    .name(n.getNodeName())
                    .gender(n.getGender())
                    .version(n.getVersion())
                    .build();
        });

        when(nodePartnerRepository.findAllByNode(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("createNode records audit with action CREATE and after snapshot")
    void createNode_recordsAudit() {
        when(nodeRepo.findByNodeIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(parentNode));
        when(nodeRepo.findMaxNodeId()).thenReturn(10L);
        when(nodeRepo.save(any(Node.class))).thenAnswer(inv -> {
            Node n = inv.getArgument(0);
            n.setId(11L);
            return n;
        });

        CreateNodeRequestDto request = new CreateNodeRequestDto();
        request.setName("Child");
        request.setGender(Gender.MALE);
        request.setIsAlive(true);
        request.setFatherId(10L);

        NodeResponseDto response = nodeService.createNode(request);

        assertThat(response).isNotNull();
        assertThat(response.getNodeId()).isEqualTo(11L);

        ArgumentCaptor<NodeSnapshot> captor = ArgumentCaptor.forClass(NodeSnapshot.class);
        verify(treeAuditService).recordNodeCreate(captor.capture(), eq(1L));

        NodeSnapshot after = captor.getValue();
        assertThat(after.nodeId()).isEqualTo(11L);
        assertThat(after.nodeName()).isEqualTo("Child");
        assertThat(after.fatherId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("updateNode records audit with action UPDATE and before/after snapshots")
    void updateNode_recordsAudit() {
        Node existing = new Node();
        existing.setId(20L);
        existing.setNodeId(62L);
        existing.setNodeName("Jac");
        existing.setGender(Gender.MALE);
        existing.setIsAlive(true);
        existing.setLevel(2L);
        existing.setTree(tree);
        existing.setVersion(1L);
        existing.setIsDeleted(false);
        existing.setIsExternal(false);

        when(nodeRepo.findByNodeIdAndIsDeletedFalse(62L)).thenReturn(Optional.of(existing));
        when(nodeRepo.save(any(Node.class))).thenAnswer(inv -> {
            Node n = inv.getArgument(0);
            n.setVersion(2L);
            return n;
        });

        UpdateNodeRequestDto request = new UpdateNodeRequestDto();
        request.setName("Jack");
        request.setGender(Gender.MALE);
        request.setIsAlive(true);
        request.setVersion(1L);

        NodeResponseDto response = nodeService.updateNode(62L, request);

        assertThat(response).isNotNull();

        ArgumentCaptor<NodeSnapshot> beforeCaptor = ArgumentCaptor.forClass(NodeSnapshot.class);
        ArgumentCaptor<NodeSnapshot> afterCaptor = ArgumentCaptor.forClass(NodeSnapshot.class);
        verify(treeAuditService).recordNodeUpdate(beforeCaptor.capture(), afterCaptor.capture(), eq(1L));

        assertThat(beforeCaptor.getValue().nodeName()).isEqualTo("Jac");
        assertThat(beforeCaptor.getValue().nodeId()).isEqualTo(62L);
        assertThat(afterCaptor.getValue().nodeName()).isEqualTo("Jack");
        assertThat(afterCaptor.getValue().nodeId()).isEqualTo(62L);
    }

    @Test
    @DisplayName("updateNode throws OptimisticLockingFailureException on version mismatch and does NOT record audit")
    void updateNode_versionMismatch_noAudit() {
        Node existing = new Node();
        existing.setId(20L);
        existing.setNodeId(62L);
        existing.setNodeName("Jac");
        existing.setGender(Gender.MALE);
        existing.setIsAlive(true);
        existing.setVersion(2L); // DB is at version 2
        existing.setIsDeleted(false);
        existing.setIsExternal(false);

        when(nodeRepo.findByNodeIdAndIsDeletedFalse(62L)).thenReturn(Optional.of(existing));

        UpdateNodeRequestDto request = new UpdateNodeRequestDto();
        request.setName("Jack");
        request.setGender(Gender.MALE);
        request.setIsAlive(true);
        request.setVersion(1L); // Request sent stale version 1

        assertThatThrownBy(() -> nodeService.updateNode(62L, request))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(nodeRepo, never()).save(any());
        verify(treeAuditService, never()).recordNodeUpdate(any(), any(), any());
    }
}
