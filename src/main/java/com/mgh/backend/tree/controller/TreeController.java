package com.mgh.backend.tree.controller;


import com.mgh.backend.tree.domain.dto.TreeDTO;
import com.mgh.backend.tree.domain.dto.TreeWithNodesDTO;
import com.mgh.backend.tree.domain.entity.Tree;
import com.mgh.backend.tree.service.TreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trees")
@CrossOrigin(origins = {"http://localhost:4200", "https://rahem-social.web.app"}) // should be removed and configured with the filter chain
@RequiredArgsConstructor
public class TreeController {

    private final TreeService treeService;

    @GetMapping("/welcome")
    public ResponseEntity<String> getGreet() {
        return ResponseEntity.ok("Welcome");
    }
    @GetMapping("/{treeId}")
    public ResponseEntity<TreeWithNodesDTO> getTree(@PathVariable Long treeId) {
        TreeWithNodesDTO tree = treeService.getNodesWithTreeId(treeId);
        return ResponseEntity.ok(tree);
    }

    @PostMapping("/create/tree")
    public ResponseEntity<Tree> createTree(@RequestBody TreeDTO treeDTO) {
        Tree tree = treeService.createTree(treeDTO);
        return ResponseEntity.ok(tree);
    }


    @PostMapping("/create-tree/nodes")
    public ResponseEntity<String> addNodes(@RequestBody TreeWithNodesDTO treeWithNodesDTO) {
     treeService.createTreeWithNodes(treeWithNodesDTO);
        return ResponseEntity.ok("Tree Created Successfully ");
    }




}