package com.mgh.backend.tree.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PartnerVisibilityRequest {

    @NotNull
    private Boolean visible;
}
