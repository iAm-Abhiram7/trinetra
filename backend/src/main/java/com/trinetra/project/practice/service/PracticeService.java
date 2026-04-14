package com.trinetra.project.practice.service;

import com.trinetra.project.practice.dto.request.SubmitExamRequest;
import com.trinetra.project.practice.dto.response.ExamQuestionResponse;
import com.trinetra.project.practice.dto.response.ExamResultResponse;
import com.trinetra.project.practice.dto.response.ExamSubmitResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface PracticeService {

    /**
     * Returns a single question for a topic-based practice exam.
     */
    ExamQuestionResponse getPracticeExamQuestion(HttpServletRequest request, String topic, int questionIndex);

    /**
     * Submits a complete practice attempt and appends it to aptitude history.
     */
    ExamSubmitResponse submitPracticeExam(HttpServletRequest request, String topic, SubmitExamRequest requestBody);

    /**
     * Returns latest or paginated practice results for a topic.
     */
    ExamResultResponse getPracticeResults(HttpServletRequest request, String topic, int page, int limit, boolean all);
}
