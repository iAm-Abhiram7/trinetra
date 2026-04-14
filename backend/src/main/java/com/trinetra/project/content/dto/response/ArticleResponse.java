package com.trinetra.project.content.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {

    private String topic;
    private String slug;
    private String content;
    private List<ArticleExampleResponse> examples;
    private List<String> relatedTopics;
}
