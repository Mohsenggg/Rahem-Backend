package com.mgh.backend.invitation.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationQuestionResponseDto {

    private boolean blocked;
    private String message;

    /** When quiz already passed — client should call invitation generate. */
    private Boolean quizPassed;
    private String nextStep;

    private Long questionId;
    private String question;
    private List<String> options;
}
