package com.mgh.backend.invitation.repository;

import com.mgh.backend.invitation.domain.entity.InvitationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationSessionRepository extends JpaRepository<InvitationSession, Long> {

    Optional<InvitationSession> findByUserId(Long userId);
}
