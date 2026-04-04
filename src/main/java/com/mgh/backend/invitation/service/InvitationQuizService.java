package com.mgh.backend.invitation.service;

import com.mgh.backend.invitation.domain.dto.InvitationAnswerResponseDto;
import com.mgh.backend.invitation.domain.dto.InvitationQuestionResponseDto;
import com.mgh.backend.invitation.domain.entity.InvitationQuestion;
import com.mgh.backend.invitation.domain.entity.InvitationSession;
import com.mgh.backend.invitation.domain.enums.InvitationQuizNextStep;
import com.mgh.backend.invitation.repository.InvitationQuestionRepository;
import com.mgh.backend.invitation.repository.InvitationSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InvitationQuizService {

    private final InvitationQuestionRepository questionRepository;
    private final InvitationSessionRepository sessionRepository;

    private final int sessionTtlMinutes;

    public InvitationQuizService(
            InvitationQuestionRepository questionRepository,
            InvitationSessionRepository sessionRepository,
            @Value("${invitation.quiz.session-ttl-minutes:10}") int sessionTtlMinutes) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    @Transactional
    public InvitationQuestionResponseDto getQuestion(Long userId) {
        InvitationSession session = sessionRepository.findByUserId(userId)
                .orElseGet(() -> createFreshSession(userId));

        refreshIfExpired(session);

        if (session.isBlocked()) {
            return InvitationQuestionResponseDto.builder()
                    .blocked(true)
                    .message("You are blocked from generating invitations due to repeated failed quiz attempts.")
                    .build();
        }

        if (session.isPassed()) {
            return InvitationQuestionResponseDto.builder()
                    .blocked(false)
                    .quizPassed(true)
                    .nextStep(InvitationQuizNextStep.GENERATE.name())
                    .message("Quiz passed. You may generate an invitation code.")
                    .build();
        }

        InvitationQuestion question = questionRepository.findById(session.getCurrentQuestionId())
                .orElseThrow(() -> new EntityNotFoundException("Quiz question not found"));

        return InvitationQuestionResponseDto.builder()
                .blocked(false)
                .questionId(question.getId())
                .question(question.getQuestion())
                .options(List.copyOf(question.getOptions()))
                .build();
    }

    @Transactional
    public InvitationAnswerResponseDto submitAnswer(Long userId, Long questionId, String answer) {
        InvitationSession session = sessionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No active quiz session; request a question first"));

        if (session.isBlocked()) {
            throw new IllegalStateException("User is blocked");
        }

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new IllegalStateException("Quiz session expired; request a new question");
        }

        if (session.isPassed()) {
            throw new IllegalStateException("Quiz already passed; use invitation generation");
        }

        if (!questionId.equals(session.getCurrentQuestionId())) {
            throw new IllegalArgumentException("Question does not match the active session");
        }

        InvitationQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));

        boolean correct = normalize(answer).equals(normalize(question.getCorrectAnswer()));

        if (correct) {
            session.setPassed(true);
            sessionRepository.save(session);
            return InvitationAnswerResponseDto.builder()
                    .correct(true)
                    .nextStep(InvitationQuizNextStep.GENERATE.name())
                    .build();
        }

        session.setAttemptCount(session.getAttemptCount() + 1);
        int attempts = session.getAttemptCount();

        if (attempts == 1) {
            sessionRepository.save(session);
            return InvitationAnswerResponseDto.builder()
                    .correct(false)
                    .nextStep(InvitationQuizNextStep.RETRY.name())
                    .build();
        }

        if (session.getRound() == 1) {
            session.setRound(2);
            session.setAttemptCount(0);
            InvitationQuestion next = pickRandomQuestionExcluding(session.getCurrentQuestionId());
            session.setCurrentQuestionId(next.getId());
            sessionRepository.save(session);
            return InvitationAnswerResponseDto.builder()
                    .correct(false)
                    .nextStep(InvitationQuizNextStep.NEW_QUESTION.name())
                    .build();
        }

        session.setBlocked(true);
        sessionRepository.save(session);
        return InvitationAnswerResponseDto.builder()
                .correct(false)
                .nextStep(InvitationQuizNextStep.BLOCKED.name())
                .build();
    }

    @Transactional(readOnly = true)
    public void assertEligibleForInvitationGeneration(Long userId) {
        InvitationSession session = sessionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("User has not passed quiz"));

        if (session.isBlocked()) {
            throw new IllegalStateException("User is blocked");
        }

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new IllegalStateException("Quiz session expired; complete the quiz again");
        }

        if (!session.isPassed()) {
            throw new IllegalStateException("User has not passed quiz");
        }
    }

    @Transactional
    public void consumePassedSessionAfterSuccessfulInvite(Long userId) {
        sessionRepository.findByUserId(userId).ifPresent(session -> {
            session.setPassed(false);
            session.setBlocked(false);
            session.setRound(1);
            session.setAttemptCount(0);
            InvitationQuestion next = pickRandomQuestion();
            session.setCurrentQuestionId(next.getId());
            session.setExpiresAt(LocalDateTime.now().plusMinutes(sessionTtlMinutes));
            sessionRepository.save(session);
        });
    }

    private void refreshIfExpired(InvitationSession session) {
        if (!session.isBlocked() && LocalDateTime.now().isAfter(session.getExpiresAt())) {
            session.setPassed(false);
            session.setRound(1);
            session.setAttemptCount(0);
            InvitationQuestion next = pickRandomQuestion();
            session.setCurrentQuestionId(next.getId());
            session.setExpiresAt(LocalDateTime.now().plusMinutes(sessionTtlMinutes));
            sessionRepository.save(session);
        }
    }

    private InvitationSession createFreshSession(Long userId) {
        if (questionRepository.count() == 0) {
            throw new IllegalStateException("No quiz questions configured");
        }
        InvitationQuestion first = pickRandomQuestion();
        InvitationSession session = InvitationSession.builder()
                .userId(userId)
                .currentQuestionId(first.getId())
                .attemptCount(0)
                .round(1)
                .passed(false)
                .blocked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(sessionTtlMinutes))
                .build();
        return sessionRepository.save(session);
    }

    private InvitationQuestion pickRandomQuestion() {
        List<InvitationQuestion> all = questionRepository.findAll();
        if (all.isEmpty()) {
            throw new IllegalStateException("No quiz questions configured");
        }
        return all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }

    private InvitationQuestion pickRandomQuestionExcluding(Long excludeId) {
        List<InvitationQuestion> all = questionRepository.findAll();
        if (all.isEmpty()) {
            throw new IllegalStateException("No quiz questions configured");
        }
        List<InvitationQuestion> candidates = all.stream()
                .filter(q -> !q.getId().equals(excludeId))
                .toList();
        List<InvitationQuestion> pool = candidates.isEmpty() ? all : candidates;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
