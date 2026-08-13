package com.mgh.backend.tree.service;

import com.mgh.backend.auth.security.adapter.UserAuthAdapter;
import com.mgh.backend.tree.domain.dto.CreateNodeRequestDto;
import com.mgh.backend.tree.domain.dto.NodeResponseDto;
import com.mgh.backend.tree.domain.dto.UpdateNodeRequestDto;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.mapper.NodeMapper;
import com.mgh.backend.tree.repository.NodeRepo;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class NodeService {

    private static final Logger log = LoggerFactory.getLogger(NodeService.class);

    private final NodeRepo nodeRepo;
    private final NodeMapper nodeMapper;

    public NodeService(NodeRepo nodeRepo, NodeMapper nodeMapper) {
        this.nodeRepo = nodeRepo;
        this.nodeMapper = nodeMapper;
    }

    @Transactional
    public NodeResponseDto createNode(CreateNodeRequestDto request) {
        if (request.getFatherId() == null && request.getMotherId() == null) {
            throw new IllegalArgumentException("At least fatherId or motherId must be provided");
        }

        Long primaryParentId = request.getFatherId() != null ? request.getFatherId() : request.getMotherId();
        Node primaryParent = nodeRepo.findByNodeIdAndIsDeletedFalse(primaryParentId)
                .orElseThrow(() -> new EntityNotFoundException("Primary parent node not found"));

        if (request.getFatherId() != null && request.getMotherId() != null) {
            Long secondaryId = request.getMotherId();
            Node secondaryParent = nodeRepo.findByNodeIdAndIsDeletedFalse(secondaryId)
                    .orElseThrow(() -> new EntityNotFoundException("Secondary parent node not found"));
            if (!secondaryParent.getTree().getTreeId().equals(primaryParent.getTree().getTreeId())) {
                throw new IllegalArgumentException("Parents must belong to the same tree");
            }
        }

        if (request.getPartnerId() != null) {
            if (request.getPartnerId().equals(request.getFatherId()) || request.getPartnerId().equals(request.getMotherId())) {
                throw new IllegalArgumentException("Invalid partner relationship");
            }
        }

        Long maxNodeId = nodeRepo.findMaxNodeId();
        long newNodeId = (maxNodeId != null ? maxNodeId : 0L) + 1L;

        Long userId = currentUserId();

        Node node = new Node();
        node.setNodeId(newNodeId);
        node.setNodeName(request.getName().trim());
        node.setFatherId(request.getFatherId());
        node.setMotherId(request.getMotherId());
        node.setTree(primaryParent.getTree());
        node.setLevel(primaryParent.getLevel() + 1);
        node.setGender(request.getGender());
        node.setIsAlive(request.getIsAlive());
        node.setCreatedBy(userId);
        node.setUpdatedBy(userId);

        Node saved = nodeRepo.save(node);

        if (request.getPartnerId() != null) {
            Node partner = nodeRepo.findByNodeIdAndIsDeletedFalse(request.getPartnerId())
                    .orElseThrow(() -> new EntityNotFoundException("Partner node not found"));
            validateNewPartnerLink(saved, partner);
            establishPartnership(saved, partner);
        }

        Node reloaded = nodeRepo.findByIdAndIsDeletedFalse(saved.getId())
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));
        log.debug("Created node id={} nodeId={} under primary parent nodeId={}", reloaded.getId(), reloaded.getNodeId(), primaryParent.getNodeId());
        return nodeMapper.toResponse(reloaded);
    }

    @Transactional
    public NodeResponseDto updateNode(Long id, UpdateNodeRequestDto request) {
        Node node = nodeRepo.findByNodeIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        Long userId = currentUserId();
        node.setNodeName(request.getName().trim());
        node.setGender(request.getGender());
        node.setIsAlive(request.getIsAlive());
        node.setUpdatedBy(userId);

        Long newPartnerBusinessId = request.getPartnerId();
        Long currentPartnerBusinessId = node.getPartnerId();

        if (newPartnerBusinessId == null) {
            if (currentPartnerBusinessId != null) {
                clearPartnershipFor(node);
            }
        } else if (!Objects.equals(newPartnerBusinessId, currentPartnerBusinessId)) {
            Node partner = nodeRepo.findByNodeIdAndIsDeletedFalse(newPartnerBusinessId)
                    .orElseThrow(() -> new EntityNotFoundException("Partner node not found"));
            validateUpdatePartnerLink(node, partner);
            clearPartnershipFor(node);
            clearPartnershipFor(partner);
            establishPartnership(node, partner);
        }

        Node saved = nodeRepo.save(node);
        return toResponseWithPartner(saved);
    }

    private NodeResponseDto toResponseWithPartner(Node node) {
        NodeResponseDto dto = nodeMapper.toResponse(node);
        if (node.getPartnerId() != null) {
            nodeRepo.findByNodeIdAndIsDeletedFalse(node.getPartnerId())
                    .ifPresent(p -> dto.setPartner(nodeMapper.toPartnerSummary(p)));
        }
        return dto;
    }

    private void validateNewPartnerLink(Node newNode, Node partner) {
        if (partner.getNodeId().equals(newNode.getFatherId()) || partner.getNodeId().equals(newNode.getMotherId())) {
            throw new IllegalArgumentException("Invalid partner relationship");
        }
        if (!partner.getTree().getTreeId().equals(newNode.getTree().getTreeId())) {
            throw new IllegalArgumentException("Invalid partner relationship");
        }
        if (partner.getPartnerId() != null) {
            throw new IllegalArgumentException("Partner already assigned");
        }
    }

    private void validateUpdatePartnerLink(Node node, Node partner) {
        if (partner.getNodeId().equals(node.getNodeId())) {
            throw new IllegalArgumentException("Invalid partner relationship");
        }
        if (partner.getNodeId().equals(node.getFatherId()) || partner.getNodeId().equals(node.getMotherId())) {
            throw new IllegalArgumentException("Invalid partner relationship");
        }
        if (!partner.getTree().getTreeId().equals(node.getTree().getTreeId())) {
            throw new IllegalArgumentException("Invalid partner relationship");
        }
        if (partner.getPartnerId() != null && !partner.getPartnerId().equals(node.getNodeId())) {
            throw new IllegalArgumentException("Partner already assigned");
        }
    }

    private void establishPartnership(Node a, Node b) {
        a.setPartnerId(b.getNodeId());
        a.setPartnerName(b.getNodeName());
        b.setPartnerId(a.getNodeId());
        b.setPartnerName(a.getNodeName());
        nodeRepo.save(a);
        nodeRepo.save(b);
    }

    private void clearPartnershipFor(Node node) {
        Long otherId = node.getPartnerId();
        if (otherId == null) {
            return;
        }
        nodeRepo.findByNodeIdAndIsDeletedFalse(otherId).ifPresent(other -> {
            if (other.getPartnerId() != null && other.getPartnerId().equals(node.getNodeId())) {
                other.setPartnerId(null);
                other.setPartnerName(null);
                nodeRepo.save(other);
            }
        });
        node.setPartnerId(null);
        node.setPartnerName(null);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserAuthAdapter adapter) {
            return adapter.getUserAuth().getId();
        }
        return null;
    }
}
