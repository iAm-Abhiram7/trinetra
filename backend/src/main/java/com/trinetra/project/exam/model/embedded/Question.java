package com.trinetra.project.exam.model.embedded;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Field("text")
    private String text;

    @Field("options")
    private List<String> options;

    @Field("correctIndex")
    // SECURITY: Never include in any client-facing DTO.
    private Integer correctIndex;

    @Field("explanation")
    private String explanation;

    @Field("topic")
    private String topic;

    @Field("difficulty")
    private String difficulty;
}
