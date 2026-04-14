package com.trinetra.project.content.service;

import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.common.exception.UnauthorizedException;
import com.trinetra.project.common.security.claims.StudentClaims;
import com.trinetra.project.content.dto.response.ArticleExampleResponse;
import com.trinetra.project.content.dto.response.ArticleResponse;
import com.trinetra.project.content.model.Article;
import com.trinetra.project.content.repository.ArticleRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

@Service
public class ContentServiceImpl implements ContentService {

    private final ArticleRepository articleRepository;

    public ContentServiceImpl(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public ArticleResponse getArticle(HttpServletRequest request, String topic) {
        verifyApprovedStudent(request);

        String normalizedTopic = normalizeTopic(topic);
        Article article = articleRepository.findActiveBySlug(normalizedTopic)
            .orElseThrow(() -> new ResourceNotFoundException("No article for this topic"));

        // TODO: Add POST /admin/article endpoint in Super Admin section.
        // TODO: Support create/update article management as a Phase 2 enhancement.
        return ArticleResponse.builder()
            .topic(article.getTopic())
            .slug(article.getSlug())
            .content(article.getContent())
            .examples(
                article.getExamples() == null
                    ? new ArrayList<>()
                    : article.getExamples().stream().map(example ->
                        ArticleExampleResponse.builder()
                            .question(example.getQuestion())
                            .answer(example.getAnswer())
                            .explanation(example.getExplanation())
                            .build()
                    ).toList()
            )
            .relatedTopics(article.getRelatedTopics() == null ? new ArrayList<>() : article.getRelatedTopics())
            .build();
    }

    private StudentClaims verifyApprovedStudent(HttpServletRequest request) {
        StudentClaims claims = (StudentClaims) request.getAttribute("studentClaims");
        if (claims == null) {
            throw new UnauthorizedException("Token missing");
        }

        if (!"APPROVED".equals(claims.getApprovalStatus())) {
            throw new ForbiddenException("Account not approved");
        }

        return claims;
    }

    private String normalizeTopic(String topic) {
        if (topic == null) {
            return "";
        }
        return topic.toLowerCase().trim().replaceAll("\\s+", "-");
    }
}
