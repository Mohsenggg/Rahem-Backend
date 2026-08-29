package com.mgh.backend.tree.domain.entity;

import com.mgh.backend.tree.domain.enums.AuditAction;
import com.mgh.backend.tree.domain.enums.AuditEntityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "tree_history",
    indexes = {
        @Index(name = "idx_tree_history_tree_id", columnList = "tree_id"),
        @Index(name = "idx_tree_history_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_tree_history_time", columnList = "performed_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tree_id", nullable = false)
    private Long treeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 16)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    private AuditEntityType entityType;

    /**
     * For NODE: nodeId (business key).
     * For PARTNER: NodePartner.id (relationship row PK).
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "performed_by")
    private Long performedBy;

    @CreationTimestamp
    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    @Column(name = "previous_state", columnDefinition = "jsonb")
    private String previousState;

    @Column(name = "new_state", columnDefinition = "jsonb")
    private String newState;
}
