package com.trinetra.project.superadmin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String text;

    @NotNull(message = "Options are required")
    @Size(min = 4, max = 4, message = "Options must contain exactly 4 values")
    private List<@NotBlank(message = "Option value is required") String> options;

    @NotNull(message = "Correct index is required")
    @Min(value = 0, message = "Correct index must be between 0 and 3")
    @Max(value = 3, message = "Correct index must be between 0 and 3")
    private Integer correctIndex;

    private String explanation;

    @NotBlank(message = "Topic is required")
    private String topic;

    @NotBlank(message = "Difficulty is required")
    private String difficulty;
}
