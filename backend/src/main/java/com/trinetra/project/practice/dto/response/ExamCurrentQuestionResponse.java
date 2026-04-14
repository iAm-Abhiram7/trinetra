package com.trinetra.project.practice.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamCurrentQuestionResponse {

    private Integer index;
    private String text;
    private List<String> options;
    private String topic;
    private String difficulty;
}
