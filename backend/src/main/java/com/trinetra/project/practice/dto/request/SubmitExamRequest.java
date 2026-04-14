package com.trinetra.project.practice.dto.request;

import com.trinetra.project.common.dto.AnswerSubmission;
import jakarta.validation.Valid;
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
public class SubmitExamRequest {

    @NotBlank(message = "examId is required")
    private String examId;

    @NotNull(message = "answers are required")
    @Size(min = 1, message = "answers cannot be empty")
    private List<@Valid AnswerSubmission> answers;

    @Min(value = 0, message = "timeTaken must be >= 0")
    private Integer timeTaken;
}
