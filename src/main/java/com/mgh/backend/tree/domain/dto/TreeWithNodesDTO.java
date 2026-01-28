package com.mgh.backend.tree.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class TreeWithNodesDTO {
    private Long treeId;
    private String treeName;
    private List<NodeDTO> nodeDTOS;
}