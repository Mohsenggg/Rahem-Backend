package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResponseDto {

    private Long id;
    private Long nodeId;
    private Long parentId;
    private String name;
    private Long level;
    private Gender gender;
    private Boolean isAlive;
    private PartnerSummaryDto partner;
}
