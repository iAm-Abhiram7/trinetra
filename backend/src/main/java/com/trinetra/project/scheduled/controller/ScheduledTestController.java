package com.trinetra.project.scheduled.controller;

import com.trinetra.project.common.response.ApiResponse;
import com.trinetra.project.scheduled.dto.request.SubmitTestRequest;
import com.trinetra.project.scheduled.dto.response.TestQuestionResponse;
import com.trinetra.project.scheduled.dto.response.TestResultResponse;
import com.trinetra.project.scheduled.dto.response.TestSubmitResponse;
import com.trinetra.project.scheduled.service.ScheduledTestService;
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
@Tag(name = "Scheduled - Test")
public class ScheduledTestController {

    private final ScheduledTestService scheduledTestService;

    public ScheduledTestController(ScheduledTestService scheduledTestService) {
        this.scheduledTestService = scheduledTestService;
    }

    @Operation(summary = "Get scheduled test question", description = "Returns one scheduled test question with eligibility checks")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Question fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Access denied",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/test/{testId}")
    public ResponseEntity<ApiResponse<TestQuestionResponse>> getScheduledTestQuestion(
        HttpServletRequest request,
        @PathVariable String testId,
        @RequestParam(defaultValue = "0") int questionIndex
    ) {
        TestQuestionResponse response = scheduledTestService.getScheduledTestQuestion(request, testId, questionIndex);
        return ResponseEntity.ok(ApiResponse.success("Question fetched successfully.", response));
    }

    @Operation(summary = "Submit scheduled test", description = "Scores scheduled test with negative marking and stores result")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Test submitted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Duplicate submission",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/test/{testId}/submit")
    public ResponseEntity<ApiResponse<TestSubmitResponse>> submitScheduledTest(
        HttpServletRequest request,
        @PathVariable String testId,
        @Valid @RequestBody SubmitTestRequest requestBody
    ) {
        TestSubmitResponse response = scheduledTestService.submitScheduledTest(request, testId, requestBody);
        return ResponseEntity.ok(ApiResponse.success("Test submitted successfully.", response));
    }

    @Operation(summary = "Get scheduled test result", description = "Returns logged-in student's scheduled test result")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Result fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No attempt found",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/test/{testId}/results")
    public ResponseEntity<ApiResponse<TestResultResponse>> getScheduledTestResult(
        HttpServletRequest request,
        @PathVariable String testId
    ) {
        TestResultResponse response = scheduledTestService.getScheduledTestResult(request, testId);
        return ResponseEntity.ok(ApiResponse.success("Result fetched successfully.", response));
    }
}
