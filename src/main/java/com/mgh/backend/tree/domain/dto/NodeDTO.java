package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.Gender;
import lombok.Data;

@Data
public class NodeDTO {

    private Long nodeId;
    private Long parentId;
    private Long partnerId;

    private Long level;
    private String nodeName;
    private String partnerName;

    private Gender gender;
    private Boolean isAlive;
}