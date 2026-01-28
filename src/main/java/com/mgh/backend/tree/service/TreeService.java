package com.mgh.backend.tree.service;


import com.mgh.backend.tree.domain.dto.NodeDTO;
import com.mgh.backend.tree.domain.dto.TreeDTO;
import com.mgh.backend.tree.domain.dto.TreeWithNodesDTO;
import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.domain.entity.Tree;
import com.mgh.backend.tree.repository.NodeRepo;
import com.mgh.backend.tree.repository.TreeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class TreeService {

    private final TreeRepo treeRepository;
    private final NodeRepo nodeRepository;

    // Tree operations
    public Tree createTree(TreeDTO treeDTO) {
        Tree tree = new Tree();
        tree.setTreeName(treeDTO.getTreeName());
        tree.setCreatedAt(treeDTO.getCreatedAt());
        tree.setUpdatedAt(treeDTO.getUpdatedAt());
        return treeRepository.save(tree);
    }

    @Transactional
    public void createTreeWithNodes(TreeWithNodesDTO treeWithNodesDTO) {

        Tree tree;

        // 1️⃣ Resolve or create Tree
        if (treeWithNodesDTO.getTreeId() != null) {
            tree = treeRepository.findById(treeWithNodesDTO.getTreeId())
                    .orElseGet(() -> {
                        Tree newTree = new Tree();
                        newTree.setTreeName(treeWithNodesDTO.getTreeName());
                        newTree.setCreatedAt(LocalDateTime.now());
                        return newTree;
                    });
        } else {
            tree = new Tree();
            tree.setTreeName(treeWithNodesDTO.getTreeName());
            tree.setCreatedAt(LocalDateTime.now());
        }

        tree.setUpdatedAt(LocalDateTime.now());
        Tree savedTree = treeRepository.save(tree);


        // Collect nodeIds for validation
        Set<Long> nodeIds = treeWithNodesDTO.getNodeDTOS().stream()
                .map(NodeDTO::getNodeId)
                .collect(Collectors.toSet());

        List<Node> nodes = treeWithNodesDTO.getNodeDTOS().stream().map(n -> {

            if (n.getParentId() != 0 && !nodeIds.contains(n.getParentId())) {
                throw new IllegalArgumentException(
                        "Invalid parentId: " + n.getParentId()
                );
            }

            Node node = new Node();
            node.setNodeId(n.getNodeId());
            node.setParentId(n.getParentId());
            node.setLevel(n.getLevel());
            node.setNodeName(n.getNodeName());
            node.setTree(savedTree);
            return node;
        }).toList();

        nodeRepository.saveAll(nodes);
    }


    public TreeWithNodesDTO getNodesWithTreeId(Long treeId) {

        Tree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new IllegalArgumentException("Tree not found"));

        List<NodeDTO> nodes = nodeRepository
                .findByTreeTreeIdOrderByLevelAscNodeIdAsc(treeId)
                .stream()
                .map(n -> {
                    NodeDTO dto = new NodeDTO();
                    dto.setNodeId(n.getNodeId());
                    dto.setParentId(n.getParentId());
                    dto.setLevel(n.getLevel());
                    dto.setNodeName(n.getNodeName());
                    return dto;
                })
                .toList();

        TreeWithNodesDTO result = new TreeWithNodesDTO();
        result.setTreeId(tree.getTreeId());
        result.setTreeName(tree.getTreeName());
        result.setNodeDTOS(nodes);

        return result;
    }
}