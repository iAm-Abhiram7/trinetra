package com.trinetra.project.content.model;

import com.trinetra.project.content.model.embedded.ArticleExample;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "articles")
public class Article {

    @Id
    private String id;

    @Field("topic")
    private String topic;

    @Field("slug")
    private String slug;

    @Field("content")
    private String content;

    @Field("examples")
    private List<ArticleExample> examples;

    @Field("relatedTopics")
    private List<String> relatedTopics;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;

    @Field("isDeleted")
    private Boolean isDeleted;
}
