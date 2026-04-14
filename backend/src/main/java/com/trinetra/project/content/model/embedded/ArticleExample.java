package com.trinetra.project.content.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleExample {

    @Field("question")
    private String question;

    @Field("answer")
    private String answer;

    @Field("explanation")
    private String explanation;
}
