package com.mgh.backend.tree.service;

import com.mgh.backend.auth.security.adapter.UserAuthAdapter;
import com.mgh.backend.tree.domain.dto.CreateNodeRequestDto;
import com.mgh.backend.tree.domain.dto.NodeResponseDto;
import com.mgh.backend.tree.domain.dto.UpdateNodeRequestDto;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.mapper.NodeMapper;
import com.mgh.backend.tree.repository.NodePartnerRepository;
import com.mgh.backend.tree.repository.NodeRepo;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NodeService {

    private static final Logger log = LoggerFactory.getLogger(NodeService.class);

    private final NodeRepo nodeRepo;
    private final NodeMapper nodeMapper;
    private final NodePartnerRepository nodePartnerRepository;
    private final NodePartnerService nodePartnerService;

    public NodeService(NodeRepo nodeRepo,
                       NodeMapper nodeMapper,
                       NodePartnerRepository nodePartnerRepository,
                       NodePartnerService nodePartnerService) {
        this.nodeRepo = nodeRepo;
        this.nodeMapper = nodeMapper;
        this.nodePartnerRepository = nodePartnerRepository;
        this.nodePartnerService = nodePartnerService;
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
            Node secondaryParent = nodeRepo.findByNodeIdAndIsDeletedFalse(request.getMotherId())
                    .orElseThrow(() -> new EntityNotFoundException("Secondary parent node not found"));
            if (!secondaryParent.getTree().getTreeId().equals(primaryParent.getTree().getTreeId())) {
                throw new IllegalArgumentException("Parents must belong to the same tree");
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
        log.debug("Created node id={} nodeId={} under primary parent nodeId={}", saved.getId(), saved.getNodeId(), primaryParent.getNodeId());

        return buildResponse(saved);
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

        Node saved = nodeRepo.save(node);
        return buildResponse(saved);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private NodeResponseDto buildResponse(Node node) {
        NodeResponseDto dto = nodeMapper.toResponse(node);
        // Populate partner list from the NodePartner table
        dto.setPartners(
            nodePartnerRepository.findAllByNode(node)
                .stream()
                .map(np -> nodePartnerService.toDto(np, node))
                .toList()
        );
        return dto;
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
