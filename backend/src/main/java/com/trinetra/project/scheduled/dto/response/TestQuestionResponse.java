package com.trinetra.project.scheduled.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestionResponse {

    private String testId;
    private String title;
    private String type;
    private Integer totalQuestions;
    private Integer totalMarks;
    private Double negativeMarking;
    private Integer durationMinutes;
    private ScheduledWindowResponse scheduledWindow;
    private TestCurrentQuestionResponse currentQuestion;
    private Boolean hasNext;
}
