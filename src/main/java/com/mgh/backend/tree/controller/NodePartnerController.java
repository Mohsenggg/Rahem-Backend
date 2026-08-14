package com.mgh.backend.tree.controller;

import com.mgh.backend.tree.domain.dto.CreateGhostNodeRequest;
import com.mgh.backend.tree.domain.dto.CreateNodePartnerRequest;
import com.mgh.backend.tree.domain.dto.NodePartnerDto;
import com.mgh.backend.tree.domain.dto.PartnerVisibilityRequest;
import com.mgh.backend.tree.domain.dto.UpdateNodePartnerRequest;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.domain.enums.PartnerStatus;
import com.mgh.backend.tree.repository.NodeRepo;
import com.mgh.backend.tree.service.NodePartnerService;
import com.mgh.backend.tree.service.NodeService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nodes/{nodeId}/partners")
@RequiredArgsConstructor
public class NodePartnerController {

    private final NodePartnerService nodePartnerService;
    private final NodeService nodeService;
    private final NodeRepo nodeRepo;

    /**
     * GET /api/nodes/{nodeId}/partners
     * Returns all partner relationships for the given node.
     */
    @GetMapping
    public ResponseEntity<List<NodePartnerDto>> getPartners(@PathVariable Long nodeId) {
        return ResponseEntity.ok(nodePartnerService.getPartners(nodeId));
    }

    /**
     * POST /api/nodes/{nodeId}/partners
     * Create a new partnership between nodeId and the specified partner node.
     */
    @PostMapping
    public ResponseEntity<NodePartnerDto> addPartner(
            @PathVariable Long nodeId,
            @Valid @RequestBody CreateNodePartnerRequest request) {
        NodePartnerDto created = nodePartnerService.addPartner(nodeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/nodes/{nodeId}/partners/{partnershipId}
     * Update status, visibility, and dates of an existing partnership.
     */
    @PutMapping("/{partnershipId}")
    public ResponseEntity<NodePartnerDto> updatePartner(
            @PathVariable Long nodeId,
            @PathVariable Long partnershipId,
            @RequestBody UpdateNodePartnerRequest request) {
        return ResponseEntity.ok(nodePartnerService.updatePartner(nodeId, partnershipId, request));
    }

    /**
     * PATCH /api/nodes/{nodeId}/partners/{partnershipId}/visibility
     * Toggle the visibility of a partnership without changing its status.
     */
    @PatchMapping("/{partnershipId}/visibility")
    public ResponseEntity<NodePartnerDto> setVisibility(
            @PathVariable Long nodeId,
            @PathVariable Long partnershipId,
            @Valid @RequestBody PartnerVisibilityRequest request) {
        return ResponseEntity.ok(nodePartnerService.setVisibility(nodeId, partnershipId, request));
    }

    /**
     * DELETE /api/nodes/{nodeId}/partners/{partnershipId}
     * Permanently remove a partnership.
     */
    @DeleteMapping("/{partnershipId}")
    public ResponseEntity<Void> removePartner(
            @PathVariable Long nodeId,
            @PathVariable Long partnershipId) {
        nodePartnerService.removePartner(nodeId, partnershipId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/nodes/{nodeId}/partners/external
     * Create an external "ghost" node and link it as a partner.
     */
    @PostMapping("/external")
    public ResponseEntity<NodePartnerDto> addExternalPartner(
            @PathVariable Long nodeId,
            @Valid @RequestBody CreateGhostNodeRequest request) {
        
        Node primaryNode = nodeRepo.findByNodeIdAndIsDeletedFalse(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found: nodeId=" + nodeId));

        Node ghostNode = nodeService.createGhostNode(request, primaryNode);

        CreateNodePartnerRequest linkReq = new CreateNodePartnerRequest();
        linkReq.setPartnerNodeId(ghostNode.getNodeId());
        linkReq.setStatus(PartnerStatus.ACTIVE);
        linkReq.setIsVisible(true);

        NodePartnerDto created = nodePartnerService.addPartner(nodeId, linkReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
