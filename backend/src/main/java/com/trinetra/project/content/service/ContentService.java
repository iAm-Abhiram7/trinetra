package com.trinetra.project.content.service;

import com.trinetra.project.content.dto.response.ArticleResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ContentService {

    /**
     * Returns an article by normalized topic slug for an approved student.
     */
    ArticleResponse getArticle(HttpServletRequest request, String topic);
}
