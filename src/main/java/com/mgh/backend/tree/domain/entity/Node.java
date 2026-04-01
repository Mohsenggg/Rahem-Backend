package com.mgh.backend.tree.domain.entity;

import com.mgh.backend.tree.domain.enums.Gender;
import com.mgh.backend.tree.domain.enums.TreeNodeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
@Entity
@Table(name = "node")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    // DB identity (internal)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Business identity (tree logic)
    @Column(name = "node_id", nullable = false, unique = true)
    private Long nodeId;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "node_parent_name", nullable = false)
    private String nodeParentName; // familyName

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "partner_name")
    private String partnerName;

    @Column(name = "user_id")
    private Long userId; // Link to the authentication user once registration is approved.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id", nullable = false)
    private Tree tree;

    @Column(name = "level", nullable = false)
    private Long level;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "invitation_code", length = 512)
    private String invitationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TreeNodeStatus status = TreeNodeStatus.INACTIVE;

    @Column(name = "is_alive")
    private Boolean isAlive;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy; // userId

    @Column(name = "updated_by")
    private Long updatedBy; // userId
}
