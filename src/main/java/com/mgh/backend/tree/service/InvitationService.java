package com.mgh.backend.tree.service;

import com.mgh.backend.tree.domain.dto.InvitationCodeGenerateRequestDto;
import com.mgh.backend.tree.domain.dto.InvitationCodeResponseDto;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.domain.enums.TreeNodeStatus;
import com.mgh.backend.tree.repository.NodeRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class InvitationService {

    private final NodeRepo nodeRepo;

    public InvitationService(NodeRepo nodeRepo) {
        this.nodeRepo = nodeRepo;
    }

    public InvitationCodeResponseDto generateInvitationCode(InvitationCodeGenerateRequestDto request) {

        Long nodeId = request.getNodeId();
        Node node = nodeRepo.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        String payload = "nodeId:" + node.getId();
        String encrypted = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        node.setInvitationCode(encrypted);
        node.setStatus(TreeNodeStatus.INACTIVE);
        nodeRepo.save(node);

        return new InvitationCodeResponseDto(encrypted);
    }

    public Long validateAndExtractNodeId(String invitationCode) {
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(invitationCode), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid invitation code");
        }

        if (!decoded.startsWith("nodeId:")) {
            throw new IllegalArgumentException("Invalid invitation code");
        }

        String idPart = decoded.substring("nodeId:".length());
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid invitation code");
        }
    }
}

