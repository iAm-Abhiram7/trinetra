package com.trinetra.project.superadmin.dto.request;

import com.trinetra.project.superadmin.dto.response.ScheduledWindowDto;
import jakarta.validation.Valid;
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
public class CreateExamRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Type is required")
    private String type;

    private String state;
    private String college;
    private String branch;
    private Integer yearOfPassing;

    private ScheduledWindowDto scheduledWindow;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    @NotNull(message = "Total questions is required")
    private Integer totalQuestions;

    @NotNull(message = "Total marks is required")
    private Integer totalMarks;

    private Double negativeMarking;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Boolean isPublished;

    @NotNull(message = "Questions are required")
    @Size(min = 1, message = "At least one question is required")
    private List<@Valid CreateQuestionRequest> questions;
}
