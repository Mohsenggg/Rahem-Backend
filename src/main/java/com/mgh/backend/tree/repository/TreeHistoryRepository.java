package com.mgh.backend.tree.repository;

import com.mgh.backend.tree.domain.entity.TreeHistory;
import com.mgh.backend.tree.domain.enums.AuditAction;
import com.mgh.backend.tree.domain.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TreeHistoryRepository extends JpaRepository<TreeHistory, Long> {

    @Query("""
        SELECT th FROM TreeHistory th
        WHERE th.treeId = :treeId
          AND (:action IS NULL OR th.action = :action)
          AND (:entityType IS NULL OR th.entityType = :entityType)
          AND (:entityId IS NULL OR th.entityId = :entityId)
        ORDER BY th.performedAt DESC
    """)
    Page<TreeHistory> findHistoryWithFilters(
            @Param("treeId") Long treeId,
            @Param("action") AuditAction action,
            @Param("entityType") AuditEntityType entityType,
            @Param("entityId") Long entityId,
            Pageable pageable
    );
}
