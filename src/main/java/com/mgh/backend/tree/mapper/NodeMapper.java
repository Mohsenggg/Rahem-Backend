package com.mgh.backend.tree.mapper;

import com.mgh.backend.tree.domain.dto.NodeResponseDto;
import com.mgh.backend.tree.domain.dto.PartnerSummaryDto;
import com.mgh.backend.tree.domain.entity.Node;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NodeMapper {

    @Mapping(target = "name", source = "nodeName")
    @Mapping(target = "partner", ignore = true)
    NodeResponseDto toResponse(Node node);

    @Mapping(target = "id", source = "nodeId")
    @Mapping(target = "name", source = "nodeName")
    PartnerSummaryDto toPartnerSummary(Node partner);
}
