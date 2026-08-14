package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.PartnerStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateNodePartnerRequest {

    private PartnerStatus status;

    private Boolean isVisible;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
