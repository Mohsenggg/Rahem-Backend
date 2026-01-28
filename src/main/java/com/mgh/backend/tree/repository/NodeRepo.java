package com.mgh.backend.tree.repository;


import com.mgh.backend.tree.domain.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeRepo extends JpaRepository<Node, Long> {
    List<Node> findByTreeTreeId(Long treeId);

    List<Node> findByTreeTreeIdOrderByLevelAscNodeIdAsc(Long treeId);

}
