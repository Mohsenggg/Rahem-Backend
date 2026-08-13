package com.mgh.backend.tree.domain.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tree")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tree_id")
    private Long treeId;

    @Column(name = "tree_name", nullable = false)
    private String treeName;

    // One-to-Many relationship with Node
    @OneToMany(mappedBy = "tree", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Node> nodes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method to add node
    public void addNode(Node node) {
        nodes.add(node);
        node.setTree(this);
    }

    // Helper method to remove node
    public void removeNode(Node node) {
        nodes.remove(node);
        node.setTree(null);
    }
}
