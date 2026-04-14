package com.trinetra.project.content.controller;

import com.trinetra.project.common.response.ApiResponse;
import com.trinetra.project.content.dto.response.ArticleResponse;
import com.trinetra.project.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/article")
@Tag(name = "Content - Articles")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @Operation(summary = "Get article by topic slug", description = "Returns article content for an approved student")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Article fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Article not found",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{topic}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticle(
        HttpServletRequest request,
        @PathVariable String topic
    ) {
        ArticleResponse response = contentService.getArticle(request, topic);
        return ResponseEntity.ok(ApiResponse.success("Article fetched successfully.", response));
    }
}
