package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.PartnerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateNodePartnerRequest {

    /**
     * The business nodeId of the person to link as partner.
     */
    @NotNull(message = "partnerNodeId must not be null")
    private Long partnerNodeId;

    private PartnerStatus status = PartnerStatus.ACTIVE;

    private Boolean isVisible = true;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
