package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResponseDto {

    private Long id;
    private Long nodeId;
    private Long fatherId;
    private Long motherId;
    private String name;
    private Long level;
    private Gender gender;
    private Boolean isAlive;

    /** All partner relationships for this node. */
    private List<NodePartnerDto> partners;
}
