package com.mgh.backend.invitation.repository;

import com.mgh.backend.invitation.domain.entity.InvitationQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationQuestionRepository extends JpaRepository<InvitationQuestion, Long> {
}
