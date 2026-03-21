package com.mgh.backend.tree.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegistrationSubmitRequestDto {

    @NotBlank
    private String invitationCode;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String password;

    private String username;

    @Email
    private String email;

    @NotNull
    @Past
    private LocalDate birthDate;

    @NotBlank
    private String gender;

    private String address;
}

