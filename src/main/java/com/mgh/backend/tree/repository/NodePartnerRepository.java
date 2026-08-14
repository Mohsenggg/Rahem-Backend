package com.mgh.backend.tree.repository;

import com.mgh.backend.tree.domain.entity.Node;
import com.mgh.backend.tree.domain.entity.NodePartner;
import com.mgh.backend.tree.domain.enums.PartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodePartnerRepository extends JpaRepository<NodePartner, Long> {

    /**
     * Find all partnerships for a given node, regardless of which side of the canonical
     * ordering the node is on.
     */
    @Query("SELECT np FROM NodePartner np WHERE np.node = :node OR np.partner = :node")
    List<NodePartner> findAllByNode(@Param("node") Node node);

    /**
     * Find all ACTIVE partnerships for a given node (both sides).
     */
    @Query("SELECT np FROM NodePartner np WHERE (np.node = :node OR np.partner = :node) AND np.status = :status")
    List<NodePartner> findAllByNodeAndStatus(@Param("node") Node node, @Param("status") PartnerStatus status);

    /**
     * Find all visible partnerships for a given node (both sides).
     */
    @Query("SELECT np FROM NodePartner np WHERE (np.node = :node OR np.partner = :node) AND np.isVisible = true")
    List<NodePartner> findAllVisibleByNode(@Param("node") Node node);

    /**
     * Find the existing relationship record between two nodes.
     * Since canonical ordering is node.nodeId < partner.nodeId, we check
     * both possible orderings to find the single record.
     */
    @Query("SELECT np FROM NodePartner np WHERE (np.node = :a AND np.partner = :b) OR (np.node = :b AND np.partner = :a)")
    Optional<NodePartner> findByTwoNodes(@Param("a") Node a, @Param("b") Node b);

    /**
     * Check whether a relationship already exists between two nodes.
     */
    @Query("SELECT COUNT(np) > 0 FROM NodePartner np WHERE (np.node = :a AND np.partner = :b) OR (np.node = :b AND np.partner = :a)")
    boolean existsByTwoNodes(@Param("a") Node a, @Param("b") Node b);

    /**
     * Find active, visible partners for a node — used for the tree display.
     * Returns at most one result per node; the first visible active partner.
     */
    @Query("SELECT np FROM NodePartner np WHERE (np.node = :node OR np.partner = :node) AND np.status = 'ACTIVE' AND np.isVisible = true")
    List<NodePartner> findActiveVisibleByNode(@Param("node") Node node);
}
