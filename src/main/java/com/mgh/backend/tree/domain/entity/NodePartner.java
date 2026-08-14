package com.mgh.backend.tree.domain.entity;

import com.mgh.backend.tree.domain.enums.PartnerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a partnership between two nodes.
 *
 * <p>To prevent duplicate records, a canonical ordering is enforced at the service layer:
 * {@code node.nodeId < partner.nodeId}. Queries must therefore check both sides.
 */
@Entity
@Table(
    name = "node_partner",
    uniqueConstraints = @UniqueConstraint(columnNames = {"node_id", "partner_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodePartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The node with the lower nodeId (canonical "left" side of the relationship).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    /**
     * The node with the higher nodeId (canonical "right" side of the relationship).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Node partner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerStatus status = PartnerStatus.ACTIVE;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
