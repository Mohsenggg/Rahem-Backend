package com.mgh.backend.tree.mapper;

import com.mgh.backend.tree.domain.dto.NodeResponseDto;
import com.mgh.backend.tree.domain.entity.Node;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NodeMapper {

    @Mapping(target = "name", source = "nodeName")
    @Mapping(target = "partners", ignore = true) // Populated manually in NodeService
    NodeResponseDto toResponse(Node node);
}
