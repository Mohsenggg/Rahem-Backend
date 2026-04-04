package com.mgh.backend.invitation.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "invitation_session",
        uniqueConstraints = @UniqueConstraint(name = "uk_invitation_session_user", columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "current_question_id")
    private Long currentQuestionId;

    /** Wrong attempts on the current question (server-side only). */
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    /** 1 = first question round, 2 = second question after two failures in round 1. */
    @Column(nullable = false)
    @Builder.Default
    private int round = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean passed = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
