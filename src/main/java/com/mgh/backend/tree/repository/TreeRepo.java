package com.mgh.backend.tree.repository;


import com.mgh.backend.tree.domain.entity.Tree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TreeRepo extends JpaRepository<Tree, Long> {
    // Custom query methods can be added here
}