package com.mgh.backend.tree.repository;


import com.mgh.backend.tree.domain.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeRepo extends JpaRepository<Node, Long> {
    List<Node> findByTreeTreeId(Long treeId);

    List<Node> findByTreeTreeIdOrderByLevelAscNodeIdAsc(Long treeId);

    Optional<Node> findByNodeIdAndIsDeletedFalse(Long nodeId);

    Optional<Node> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT MAX(n.nodeId) FROM Node n")
    Long findMaxNodeId();
}
