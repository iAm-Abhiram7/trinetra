package com.trinetra.project.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmission {

    @NotNull(message = "questionIndex is required")
    @Min(value = 0, message = "questionIndex must be >= 0")
    private Integer questionIndex;

    @Min(value = 0, message = "selectedOption must be >= 0")
    @Max(value = 3, message = "selectedOption must be <= 3")
    private Integer selectedOption;
}
