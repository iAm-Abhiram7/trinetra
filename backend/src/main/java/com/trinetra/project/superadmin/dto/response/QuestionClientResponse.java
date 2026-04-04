package com.trinetra.project.superadmin.dto.response;

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

    private String text;
    private List<String> options;
    private String explanation;
    private String topic;
    private String difficulty;
}
