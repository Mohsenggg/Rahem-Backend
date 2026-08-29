package com.mgh.backend.tree.domain.snapshot;

import com.mgh.backend.tree.domain.entity.Node;

public record NodeSnapshot(
    Long nodeId,
    String nodeName,
    Long fatherId,
    Long motherId,
    String gender,
    Boolean isAlive,
    Long level,
    Boolean isExternal,
    Long version
) {
    public static NodeSnapshot of(Node n) {
        if (n == null) return null;
        return new NodeSnapshot(
            n.getNodeId(),
            n.getNodeName(),
            n.getFatherId(),
            n.getMotherId(),
            n.getGender() != null ? n.getGender().name() : null,
            n.getIsAlive(),
            n.getLevel(),
            n.getIsExternal(),
            n.getVersion()
        );
    }
}
