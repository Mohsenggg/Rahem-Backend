package com.mgh.backend.tree.controller;


import com.mgh.backend.tree.domain.dto.CreateNodeRequestDto;
import com.mgh.backend.tree.domain.dto.NodeResponseDto;
import com.mgh.backend.tree.domain.dto.UpdateNodeRequestDto;
import com.mgh.backend.tree.service.NodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @PostMapping("/create")
    public ResponseEntity<NodeResponseDto> createNode(@Valid @RequestBody CreateNodeRequestDto request) {
        return ResponseEntity.ok(nodeService.createNode(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<NodeResponseDto> updateNode(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNodeRequestDto request) {
        return ResponseEntity.ok(nodeService.updateNode(id, request));
    }
}
