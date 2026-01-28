package com.mgh.backend.tree.domain.dto;

import lombok.Data;

@Data
public class NodeDTO {

    private Long nodeId;
    private Long parentId;
    private Long level;
    private String nodeName;
}