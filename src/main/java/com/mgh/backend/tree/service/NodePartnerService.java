package com.mgh.backend.tree.service;

import com.mgh.backend.tree.domain.dto.*;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.domain.entity.NodePartner;
import com.mgh.backend.tree.domain.enums.Gender;
import com.mgh.backend.tree.domain.enums.PartnerStatus;
import com.mgh.backend.tree.repository.NodePartnerRepository;
import com.mgh.backend.tree.repository.NodeRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NodePartnerService {

    private final NodePartnerRepository nodePartnerRepository;
    private final NodeRepo nodeRepo;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Return all partner relationships for a given node (both sides of canonical ordering).
     */
    @Transactional(readOnly = true)
    public List<NodePartnerDto> getPartners(Long nodeId) {
        Node node = findNode(nodeId);
        return nodePartnerRepository.findAllByNode(node)
                .stream()
                .map(np -> toDto(np, node))
                .toList();
    }

    /**
     * Create a new partnership between nodeId and request.partnerNodeId.
     * Canonical ordering: the node with the lower nodeId is stored as "node".
     */
    public NodePartnerDto addPartner(Long nodeId, CreateNodePartnerRequest request) {
        Node node = findNode(nodeId);
        Node partner = findNode(request.getPartnerNodeId());

        validateSelf(node, partner);
        validateSameTree(node, partner);
        validateDuplicate(node, partner);
        validateGender(node, partner);
        validateNotParent(node, partner);
        validateNotChild(node, partner);

        // Female active-partner restriction: check both sides
        if (node.getGender() == Gender.FEMALE && request.getStatus() == PartnerStatus.ACTIVE) {
            validateFemaleActiveRule(node);
        }
        if (partner.getGender() == Gender.FEMALE && request.getStatus() == PartnerStatus.ACTIVE) {
            validateFemaleActiveRule(partner);
        }

        // Canonical ordering: lower nodeId is always the "node" side
        Node canonicalNode;
        Node canonicalPartner;
        if (node.getNodeId() < partner.getNodeId()) {
            canonicalNode = node;
            canonicalPartner = partner;
        } else {
            canonicalNode = partner;
            canonicalPartner = node;
        }

        NodePartner np = new NodePartner();
        np.setNode(canonicalNode);
        np.setPartner(canonicalPartner);
        np.setStatus(request.getStatus() != null ? request.getStatus() : PartnerStatus.ACTIVE);
        np.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : Boolean.TRUE);
        np.setStartedAt(request.getStartedAt());
        np.setEndedAt(request.getEndedAt());

        NodePartner saved = nodePartnerRepository.save(np);
        return toDto(saved, node);
    }

    /**
     * Update an existing partnership record.
     */
    public NodePartnerDto updatePartner(Long nodeId, Long partnershipId, UpdateNodePartnerRequest request) {
        Node node = findNode(nodeId);
        NodePartner np = findPartnership(partnershipId);
        validateNodeBelongsToPartnership(node, np);

        // If changing to ACTIVE, re-validate the female restriction for the female side
        if (request.getStatus() == PartnerStatus.ACTIVE && np.getStatus() != PartnerStatus.ACTIVE) {
            Node femaleNode = np.getNode().getGender() == Gender.FEMALE ? np.getNode() : null;
            if (femaleNode == null && np.getPartner().getGender() == Gender.FEMALE) {
                femaleNode = np.getPartner();
            }
            if (femaleNode != null) {
                validateFemaleActiveRule(femaleNode);
            }
        }

        if (request.getStatus() != null) {
            np.setStatus(request.getStatus());
            // Auto-populate endedAt when status changes to ENDED
            if (request.getStatus() == PartnerStatus.ENDED && np.getEndedAt() == null && request.getEndedAt() == null) {
                np.setEndedAt(LocalDateTime.now());
            }
        }
        if (request.getIsVisible() != null) np.setIsVisible(request.getIsVisible());
        if (request.getStartedAt() != null) np.setStartedAt(request.getStartedAt());
        if (request.getEndedAt() != null) np.setEndedAt(request.getEndedAt());

        return toDto(nodePartnerRepository.save(np), node);
    }

    /**
     * Patch only the visibility of a partnership.
     */
    public NodePartnerDto setVisibility(Long nodeId, Long partnershipId, PartnerVisibilityRequest request) {
        Node node = findNode(nodeId);
        NodePartner np = findPartnership(partnershipId);
        validateNodeBelongsToPartnership(node, np);

        np.setIsVisible(request.getVisible());
        return toDto(nodePartnerRepository.save(np), node);
    }

    /**
     * Permanently remove a partnership record.
     */
    public void removePartner(Long nodeId, Long partnershipId) {
        Node node = findNode(nodeId);
        NodePartner np = findPartnership(partnershipId);
        validateNodeBelongsToPartnership(node, np);
        nodePartnerRepository.delete(np);
    }

    // =========================================================================
    // Validation
    // =========================================================================

    private void validateSelf(Node node, Node partner) {
        if (node.getNodeId().equals(partner.getNodeId())) {
            throw new IllegalArgumentException("A node cannot be its own partner");
        }
    }

    private void validateGender(Node node, Node partner) {
        if (node.getGender() == partner.getGender()) {
            throw new IllegalArgumentException("Partners must be of opposite genders");
        }
    }

    private void validateNotParent(Node node, Node partner) {
        if ((node.getFatherId() != null && node.getFatherId().equals(partner.getNodeId())) ||
            (node.getMotherId() != null && node.getMotherId().equals(partner.getNodeId()))) {
            throw new IllegalArgumentException("Cannot select a parent as a partner");
        }
    }

    private void validateNotChild(Node node, Node partner) {
        if ((partner.getFatherId() != null && partner.getFatherId().equals(node.getNodeId())) ||
            (partner.getMotherId() != null && partner.getMotherId().equals(node.getNodeId()))) {
            throw new IllegalArgumentException("Cannot select a child as a partner");
        }
    }

    private void validateSameTree(Node node, Node partner) {
        if (!node.getTree().getTreeId().equals(partner.getTree().getTreeId())) {
            throw new IllegalArgumentException("Partners must belong to the same tree");
        }
    }

    private void validateDuplicate(Node node, Node partner) {
        if (nodePartnerRepository.existsByTwoNodes(node, partner)) {
            throw new IllegalArgumentException("A partnership between these two nodes already exists");
        }
    }

    /**
     * A female node may only have one ACTIVE partner at a time.
     */
    private void validateFemaleActiveRule(Node femaleNode) {
        if (femaleNode.getGender() != Gender.FEMALE) return;
        List<NodePartner> activePartnerships =
                nodePartnerRepository.findAllByNodeAndStatus(femaleNode, PartnerStatus.ACTIVE);
        if (!activePartnerships.isEmpty()) {
            throw new IllegalArgumentException(
                    "Female node (nodeId=" + femaleNode.getNodeId() + ") already has an ACTIVE partner. " +
                    "End the existing partnership before adding a new active one."
            );
        }
    }

    private void validateNodeBelongsToPartnership(Node node, NodePartner np) {
        boolean belongs = np.getNode().getNodeId().equals(node.getNodeId())
                || np.getPartner().getNodeId().equals(node.getNodeId());
        if (!belongs) {
            throw new IllegalArgumentException("Partnership does not belong to this node");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Node findNode(Long nodeId) {
        return nodeRepo.findByNodeIdAndIsDeletedFalse(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found: nodeId=" + nodeId));
    }

    private NodePartner findPartnership(Long partnershipId) {
        return nodePartnerRepository.findById(partnershipId)
                .orElseThrow(() -> new EntityNotFoundException("Partnership not found: id=" + partnershipId));
    }

    /**
     * Convert a NodePartner record to a DTO from the perspective of the given requester node.
     * The "other" node is the partner of the requester.
     */
    public NodePartnerDto toDto(NodePartner np, Node requester) {
        // Determine which side is the "other" person (not the requester)
        Node other = np.getNode().getNodeId().equals(requester.getNodeId())
                ? np.getPartner()
                : np.getNode();

        return NodePartnerDto.builder()
                .id(np.getId())
                .partnerId(other.getNodeId())
                .partnerName(other.getNodeName())
                .status(np.getStatus())
                .isVisible(np.getIsVisible())
                .startedAt(np.getStartedAt())
                .endedAt(np.getEndedAt())
                .build();
    }
}
