package com.trinetra.project.scheduled.service;

import com.trinetra.project.scheduled.dto.request.SubmitTestRequest;
import com.trinetra.project.scheduled.dto.response.TestQuestionResponse;
import com.trinetra.project.scheduled.dto.response.TestResultResponse;
import com.trinetra.project.scheduled.dto.response.TestSubmitResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ScheduledTestService {

    /**
     * Returns a single question from a scheduled exam after access and window checks.
     */
    TestQuestionResponse getScheduledTestQuestion(HttpServletRequest request, String testId, int questionIndex);

    /**
     * Scores and stores a scheduled exam submission.
     */
    TestSubmitResponse submitScheduledTest(HttpServletRequest request, String testId, SubmitTestRequest requestBody);

    /**
     * Returns a student's scheduled test result for a specific test.
     */
    TestResultResponse getScheduledTestResult(HttpServletRequest request, String testId);
}
