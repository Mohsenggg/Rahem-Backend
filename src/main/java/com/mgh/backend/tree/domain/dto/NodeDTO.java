package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.Gender;
import lombok.Data;

@Data
public class NodeDTO {

    private Long nodeId;
    private Long fatherId;
    private Long motherId;

    private Long level;
    private String nodeName;

    private Gender gender;
    private Boolean isAlive;

    /**
     * The nodeId of the active, visible partner for display in the tree.
     * Populated from the node_partner table when serving the tree response.
     */
    private Long activePartnerId;
    private String activePartnerName;

    private Long version;
}