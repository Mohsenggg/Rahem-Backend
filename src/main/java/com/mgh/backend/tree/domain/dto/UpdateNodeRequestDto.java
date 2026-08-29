package com.mgh.backend.tree.domain.dto;

import com.mgh.backend.tree.domain.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateNodeRequestDto {

    @NotBlank(message = "must not be blank")
    private String name;

    @NotNull
    private Gender gender;

    @NotNull
    private Boolean isAlive;

    private Long version;
}
