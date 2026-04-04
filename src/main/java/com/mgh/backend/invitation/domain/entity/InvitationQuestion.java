package com.mgh.backend.invitation.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invitation_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1024)
    private String question;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invitation_question_option", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text", nullable = false, length = 512)
    @OrderColumn(name = "option_order")
    @Builder.Default
    private List<String> options = new ArrayList<>();

    @Column(nullable = false, length = 512)
    private String correctAnswer;
}
