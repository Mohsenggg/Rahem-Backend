package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGhostNodeRequest {
    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotNull(message = "Gender is required")
    private Gender gender;
}
