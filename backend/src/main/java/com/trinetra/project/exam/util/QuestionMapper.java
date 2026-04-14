package com.trinetra.project.exam.util;

import com.trinetra.project.exam.dto.QuestionClientResponse;
import com.trinetra.project.exam.model.embedded.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    public QuestionClientResponse toClientResponse(Question question) {
        return QuestionClientResponse.builder()
            .index(null)
            .text(question.getText())
            .options(question.getOptions())
            .topic(question.getTopic())
            .difficulty(question.getDifficulty())
            .build();
    }

    public QuestionClientResponse toClientResponse(Question question, int index) {
        return QuestionClientResponse.builder()
            .index(index)
            .text(question.getText())
            .options(question.getOptions())
            .topic(question.getTopic())
            .difficulty(question.getDifficulty())
            .build();
    }
}
