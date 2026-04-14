package com.trinetra.project.exam.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionClientResponse {

    private Integer index;
    private String text;
    private List<String> options;
    private String topic;
    private String difficulty;
}
