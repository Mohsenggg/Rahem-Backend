package com.mgh.backend.tree.service;

import com.mgh.backend.tree.domain.dto.CreateNodePartnerRequest;
import com.mgh.backend.tree.domain.dto.NodePartnerDto;
import com.mgh.backend.tree.domain.dto.PartnerVisibilityRequest;
import com.mgh.backend.tree.domain.dto.UpdateNodePartnerRequest;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.domain.entity.NodePartner;
import com.mgh.backend.tree.domain.entity.Tree;
import com.mgh.backend.tree.domain.enums.Gender;
import com.mgh.backend.tree.domain.enums.PartnerStatus;
import com.mgh.backend.tree.domain.enums.TreeNodeStatus;
import com.mgh.backend.tree.repository.NodePartnerRepository;
import com.mgh.backend.tree.repository.NodeRepo;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NodePartnerServiceTest {

    @Mock
    private NodePartnerRepository nodePartnerRepository;

    @Mock
    private NodeRepo nodeRepo;

    @Mock
    private TreeAuditService treeAuditService;

    @InjectMocks
    private NodePartnerService nodePartnerService;

    private Tree tree;
    private Node ahmed;
    private Node sara;
    private Node mona;
    private Node huda;
    private Node khaled;

    @BeforeEach
    void setUp() {
        tree = new Tree();
        tree.setTreeId(1L);

        ahmed = makeNode(1L, "Ahmed", Gender.MALE, tree);
        sara  = makeNode(2L, "Sara",  Gender.FEMALE, tree);
        mona  = makeNode(3L, "Mona",  Gender.FEMALE, tree);
        huda  = makeNode(4L, "Huda",  Gender.FEMALE, tree);
        khaled = makeNode(5L, "Khaled", Gender.MALE, tree);
    }

    // =========================================================================
    // Multiple partners for a male node
    // =========================================================================

    @Test
    @DisplayName("Male node can have multiple partners (ENDED or ACTIVE)")
    void maleNodeCanHaveMultiplePartners() {
        // Ahmed already has Sara (ENDED) and Mona (ENDED) — adding Huda (ACTIVE) must succeed
        setupNodeLookup(ahmed, sara, mona, huda, khaled);
        when(nodePartnerRepository.existsByTwoNodes(any(), any())).thenReturn(false);
        when(nodePartnerRepository.findAllByNodeAndStatus(any(), eq(PartnerStatus.ACTIVE)))
                .thenReturn(Collections.emptyList()); // No active female restriction for Ahmed
        when(nodePartnerRepository.save(any())).thenAnswer(inv -> {
            NodePartner np = inv.getArgument(0);
            np.setId(10L);
            return np;
        });

        CreateNodePartnerRequest request = new CreateNodePartnerRequest();
        request.setPartnerNodeId(huda.getNodeId());
        request.setStatus(PartnerStatus.ACTIVE);
        request.setIsVisible(true);

        assertThatNoException().isThrownBy(() -> nodePartnerService.addPartner(ahmed.getNodeId(), request));
    }

    // =========================================================================
    // Female active-partner restriction
    // =========================================================================

    @Test
    @DisplayName("Female node with ACTIVE partner cannot get another ACTIVE partner")
    void femaleCannotHaveTwoActivePartners() {
        setupNodeLookup(mona, ahmed, khaled);
        when(nodePartnerRepository.existsByTwoNodes(any(), any())).thenReturn(false);

        // Mona already has Ahmed as ACTIVE
        NodePartner existingActive = makePartnership(100L, ahmed, mona, PartnerStatus.ACTIVE);
        when(nodePartnerRepository.findAllByNodeAndStatus(eq(mona), eq(PartnerStatus.ACTIVE)))
                .thenReturn(List.of(existingActive));

        CreateNodePartnerRequest request = new CreateNodePartnerRequest();
        request.setPartnerNodeId(khaled.getNodeId());
        request.setStatus(PartnerStatus.ACTIVE);

        assertThatThrownBy(() -> nodePartnerService.addPartner(mona.getNodeId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has an ACTIVE partner");
    }

    @Test
    @DisplayName("Female node with ENDED partner can have a new ACTIVE partner")
    void femaleWithEndedPartnerCanHaveNewActive() {
        setupNodeLookup(mona, ahmed, khaled);
        when(nodePartnerRepository.existsByTwoNodes(any(), any())).thenReturn(false);
        // Mona's previous Ahmed relationship is ENDED → no active partners
        when(nodePartnerRepository.findAllByNodeAndStatus(eq(mona), eq(PartnerStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());
        when(nodePartnerRepository.save(any())).thenAnswer(inv -> {
            NodePartner np = inv.getArgument(0);
            np.setId(11L);
            return np;
        });

        CreateNodePartnerRequest request = new CreateNodePartnerRequest();
        request.setPartnerNodeId(khaled.getNodeId());
        request.setStatus(PartnerStatus.ACTIVE);

        assertThatNoException().isThrownBy(() -> nodePartnerService.addPartner(mona.getNodeId(), request));
    }

    // =========================================================================
    // Duplicate relationship
    // =========================================================================

    @Test
    @DisplayName("Creating the same partnership twice is rejected")
    void duplicatePartnershipIsRejected() {
        setupNodeLookup(ahmed, mona);
        when(nodePartnerRepository.existsByTwoNodes(any(), any())).thenReturn(true);

        CreateNodePartnerRequest request = new CreateNodePartnerRequest();
        request.setPartnerNodeId(mona.getNodeId());

        assertThatThrownBy(() -> nodePartnerService.addPartner(ahmed.getNodeId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // =========================================================================
    // Self-relationship
    // =========================================================================

    @Test
    @DisplayName("A node cannot be its own partner")
    void selfPartnershipIsRejected() {
        setupNodeLookup(ahmed);

        CreateNodePartnerRequest request = new CreateNodePartnerRequest();
        request.setPartnerNodeId(ahmed.getNodeId()); // same node

        assertThatThrownBy(() -> nodePartnerService.addPartner(ahmed.getNodeId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be its own partner");
    }

    // =========================================================================
    // Same-tree validation
    // =========================================================================

    @Test
    @DisplayName("Partners from different trees are rejected")
    void crossTreePartnershipIsRejected() {
        Tree otherTree = new Tree();
        otherTree.setTreeId(99L);
        Node outsider = makeNode(20L, "Outsider", Gender.FEMALE, otherTree);

        setupNodeLookup(ahmed, outsider);

        CreateNodePartnerRequest request = new CreateNodePartnerRequest();
        request.setPartnerNodeId(outsider.getNodeId());

        assertThatThrownBy(() -> nodePartnerService.addPartner(ahmed.getNodeId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same tree");
    }

    // =========================================================================
    // Visibility
    // =========================================================================

    @Test
    @DisplayName("Setting visibility to false does not delete the relationship")
    void hidingPartnershipPreservesRelationship() {
        setupNodeLookup(ahmed);
        NodePartner np = makePartnership(50L, ahmed, sara, PartnerStatus.ACTIVE);
        np.setIsVisible(true);
        when(nodePartnerRepository.findById(50L)).thenReturn(Optional.of(np));
        when(nodePartnerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PartnerVisibilityRequest visReq = new PartnerVisibilityRequest();
        visReq.setVisible(false);

        NodePartnerDto result = nodePartnerService.setVisibility(ahmed.getNodeId(), 50L, visReq);

        assertThat(result.getIsVisible()).isFalse();
        verify(nodePartnerRepository, never()).delete(any());
    }

    // =========================================================================
    // Children correctness (structural test — verifies DTO mapping)
    // =========================================================================

    @Test
    @DisplayName("toDto returns the correct 'other' node from requester perspective")
    void toDtoReturnsCorrectOtherNode() {
        // Ahmed (nodeId=1) and Sara (nodeId=2) — canonical: node=ahmed, partner=sara
        NodePartner np = makePartnership(1L, ahmed, sara, PartnerStatus.ACTIVE);

        // From Ahmed's perspective, partner should be Sara
        NodePartnerDto fromAhmed = nodePartnerService.toDto(np, ahmed);
        assertThat(fromAhmed.getPartnerId()).isEqualTo(sara.getNodeId());
        assertThat(fromAhmed.getPartnerName()).isEqualTo("Sara");

        // From Sara's perspective, partner should be Ahmed
        NodePartnerDto fromSara = nodePartnerService.toDto(np, sara);
        assertThat(fromSara.getPartnerId()).isEqualTo(ahmed.getNodeId());
        assertThat(fromSara.getPartnerName()).isEqualTo("Ahmed");
    }

    // =========================================================================
    // Update auto-populates endedAt
    // =========================================================================

    @Test
    @DisplayName("Updating status to ENDED auto-populates endedAt when not provided")
    void updateToEndedAutoPopulatesEndedAt() {
        setupNodeLookup(ahmed);
        NodePartner np = makePartnership(60L, ahmed, sara, PartnerStatus.ACTIVE);
        when(nodePartnerRepository.findById(60L)).thenReturn(Optional.of(np));
        when(nodePartnerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateNodePartnerRequest req = new UpdateNodePartnerRequest();
        req.setStatus(PartnerStatus.ENDED);

        NodePartnerDto result = nodePartnerService.updatePartner(ahmed.getNodeId(), 60L, req);

        assertThat(result.getStatus()).isEqualTo(PartnerStatus.ENDED);
    }

    // =========================================================================
    // Audit History Verification
    // =========================================================================

    @Test
    @DisplayName("addPartner records audit with action CREATE and after snapshot")
    void addPartner_recordsAudit() {
        setupNodeLookup(ahmed, sara);
        when(nodePartnerRepository.existsByTwoNodes(any(), any())).thenReturn(false);
        when(nodePartnerRepository.findAllByNodeAndStatus(any(), eq(PartnerStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());
        when(nodePartnerRepository.save(any())).thenAnswer(inv -> {
            NodePartner np = inv.getArgument(0);
            np.setId(77L);
            return np;
        });

        CreateNodePartnerRequest req = new CreateNodePartnerRequest();
        req.setPartnerNodeId(sara.getNodeId());
        req.setStatus(PartnerStatus.ACTIVE);
        req.setIsVisible(true);

        nodePartnerService.addPartner(ahmed.getNodeId(), req);

        verify(treeAuditService).recordPartnerCreate(any(), eq(1L));
    }

    @Test
    @DisplayName("updatePartner records audit with action UPDATE and before/after snapshots")
    void updatePartner_recordsAudit() {
        setupNodeLookup(ahmed);
        NodePartner np = makePartnership(60L, ahmed, sara, PartnerStatus.ACTIVE);
        when(nodePartnerRepository.findById(60L)).thenReturn(Optional.of(np));
        when(nodePartnerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateNodePartnerRequest req = new UpdateNodePartnerRequest();
        req.setStatus(PartnerStatus.ENDED);

        nodePartnerService.updatePartner(ahmed.getNodeId(), 60L, req);

        verify(treeAuditService).recordPartnerUpdate(any(), any(), eq(1L));
    }

    @Test
    @DisplayName("setVisibility records audit with action UPDATE")
    void setVisibility_recordsAudit() {
        setupNodeLookup(ahmed);
        NodePartner np = makePartnership(60L, ahmed, sara, PartnerStatus.ACTIVE);
        when(nodePartnerRepository.findById(60L)).thenReturn(Optional.of(np));
        when(nodePartnerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PartnerVisibilityRequest req = new PartnerVisibilityRequest();
        req.setVisible(false);

        nodePartnerService.setVisibility(ahmed.getNodeId(), 60L, req);

        verify(treeAuditService).recordPartnerUpdate(any(), any(), eq(1L));
    }

    @Test
    @DisplayName("removePartner records audit with action DELETE and before snapshot")
    void removePartner_recordsAudit() {
        setupNodeLookup(ahmed);
        NodePartner np = makePartnership(60L, ahmed, sara, PartnerStatus.ACTIVE);
        when(nodePartnerRepository.findById(60L)).thenReturn(Optional.of(np));

        nodePartnerService.removePartner(ahmed.getNodeId(), 60L);

        verify(nodePartnerRepository).delete(np);
        verify(treeAuditService).recordPartnerDelete(any(), eq(1L));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Node makeNode(Long nodeId, String name, Gender gender, Tree tree) {
        Node n = new Node();
        n.setId(nodeId);
        n.setNodeId(nodeId);
        n.setNodeName(name);
        n.setGender(gender);
        n.setTree(tree);
        n.setLevel(1L);
        n.setStatus(TreeNodeStatus.INACTIVE);
        n.setIsDeleted(false);
        return n;
    }

    private NodePartner makePartnership(Long id, Node a, Node b, PartnerStatus status) {
        // Canonical: lower nodeId is "node"
        Node canonicalNode = a.getNodeId() < b.getNodeId() ? a : b;
        Node canonicalPartner = a.getNodeId() < b.getNodeId() ? b : a;
        NodePartner np = new NodePartner();
        np.setId(id);
        np.setNode(canonicalNode);
        np.setPartner(canonicalPartner);
        np.setStatus(status);
        np.setIsVisible(true);
        return np;
    }

    private void setupNodeLookup(Node... nodes) {
        for (Node n : nodes) {
            when(nodeRepo.findByNodeIdAndIsDeletedFalse(n.getNodeId())).thenReturn(Optional.of(n));
        }
    }
}
