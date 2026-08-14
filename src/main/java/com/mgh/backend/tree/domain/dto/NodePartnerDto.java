package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.PartnerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodePartnerDto {

    /** DB id of the NodePartner relationship record. */
    private Long id;

    /** The nodeId (business ID) of the other person in this partnership. */
    private Long partnerId;

    /** Display name of the other person. */
    private String partnerName;

    private PartnerStatus status;

    private Boolean isVisible;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
