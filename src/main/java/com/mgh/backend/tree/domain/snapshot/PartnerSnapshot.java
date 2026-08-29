package com.mgh.backend.tree.domain.snapshot;

import com.mgh.backend.tree.domain.entity.NodePartner;

import java.time.LocalDateTime;

public record PartnerSnapshot(
    Long partnershipId,
    Long nodeId,
    Long partnerId,
    String status,
    Boolean isVisible,
    LocalDateTime startedAt,
    LocalDateTime endedAt
) {
    public static PartnerSnapshot of(NodePartner np) {
        if (np == null) return null;
        return new PartnerSnapshot(
            np.getId(),
            np.getNode() != null ? np.getNode().getNodeId() : null,
            np.getPartner() != null ? np.getPartner().getNodeId() : null,
            np.getStatus() != null ? np.getStatus().name() : null,
            np.getIsVisible(),
            np.getStartedAt(),
            np.getEndedAt()
        );
    }
}
