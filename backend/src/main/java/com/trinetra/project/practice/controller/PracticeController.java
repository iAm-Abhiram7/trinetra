package com.trinetra.project.practice.controller;

import com.trinetra.project.common.response.ApiResponse;
import com.trinetra.project.practice.dto.request.SubmitExamRequest;
import com.trinetra.project.practice.dto.response.ExamQuestionResponse;
import com.trinetra.project.practice.dto.response.ExamResultResponse;
import com.trinetra.project.practice.dto.response.ExamSubmitResponse;
import com.trinetra.project.practice.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Practice - Exam")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @Operation(summary = "Get practice question", description = "Returns one question at a time from a practice exam")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Question fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Practice exam not found",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/exam/{topic}")
    public ResponseEntity<ApiResponse<ExamQuestionResponse>> getPracticeExamQuestion(
        HttpServletRequest request,
        @PathVariable String topic,
        @RequestParam(defaultValue = "0") int questionIndex
    ) {
        ExamQuestionResponse response = practiceService.getPracticeExamQuestion(request, topic, questionIndex);
        return ResponseEntity.ok(ApiResponse.success("Question fetched successfully.", response));
    }

    @Operation(summary = "Submit practice exam", description = "Scores and stores a practice attempt")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Practice exam submitted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/exam/submit/{topic}")
    public ResponseEntity<ApiResponse<ExamSubmitResponse>> submitPracticeExam(
        HttpServletRequest request,
        @PathVariable String topic,
        @Valid @RequestBody SubmitExamRequest requestBody
    ) {
        ExamSubmitResponse response = practiceService.submitPracticeExam(request, topic, requestBody);
        return ResponseEntity.ok(ApiResponse.success("Practice exam submitted successfully.", response));
    }

    @Operation(summary = "Get practice results", description = "Returns latest or paginated attempts for a practice topic")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Results fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No attempts found",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/result/exam/{topic}")
    public ResponseEntity<ApiResponse<ExamResultResponse>> getPracticeResults(
        HttpServletRequest request,
        @PathVariable String topic,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(defaultValue = "false") boolean all
    ) {
        ExamResultResponse response = practiceService.getPracticeResults(request, topic, page, limit, all);
        return ResponseEntity.ok(ApiResponse.success("Practice results fetched successfully.", response));
    }
}
