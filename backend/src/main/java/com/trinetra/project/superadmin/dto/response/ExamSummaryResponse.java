package com.trinetra.project.superadmin.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSummaryResponse {

    private String id;
    private String title;
    private String type;
    private String state;
    private String college;
    private String branch;
    private Integer yearOfPassing;
    private ScheduledWindowDto scheduledWindow;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private Integer totalMarks;
    private Double negativeMarking;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Boolean isPublished;
    private String createdBy;
    private Instant createdAt;
}
