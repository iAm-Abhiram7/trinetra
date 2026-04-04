package com.trinetra.project.superadmin.service;

import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.common.exception.UnprocessableException;
import com.trinetra.project.common.exception.ValidationException;
import com.trinetra.project.common.security.claims.SuperAdminClaims;
import com.trinetra.project.exam.model.Exam;
import com.trinetra.project.exam.model.embedded.Question;
import com.trinetra.project.exam.model.embedded.ScheduledWindow;
import com.trinetra.project.exam.repository.ExamRepository;
import com.trinetra.project.superadmin.dto.request.CreateExamRequest;
import com.trinetra.project.superadmin.dto.request.CreateQuestionRequest;
import com.trinetra.project.superadmin.dto.response.DeleteTestResponse;
import com.trinetra.project.superadmin.dto.response.ExamDetailResponse;
import com.trinetra.project.superadmin.dto.response.ExamSummaryResponse;
import com.trinetra.project.superadmin.dto.response.PagedTestsResponse;
import com.trinetra.project.superadmin.dto.response.QuestionClientResponse;
import com.trinetra.project.superadmin.dto.response.ScheduledWindowDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SuperAdminTestServiceImpl implements SuperAdminTestService {

    private final MongoTemplate examsMongoTemplate;
    private final ExamRepository examRepository;

    public SuperAdminTestServiceImpl(
        @Qualifier("examsMongoTemplate") MongoTemplate examsMongoTemplate,
        ExamRepository examRepository
    ) {
        this.examsMongoTemplate = examsMongoTemplate;
        this.examRepository = examRepository;
    }

    @Override
    public PagedTestsResponse getAllTests(
        HttpServletRequest request,
        int page,
        int limit,
        String type,
        Boolean isPublished,
        String search
    ) {
        SuperAdminClaims claims = verifySuperAdmin(request);

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 20 : limit;

        Query query = new Query();
        query.addCriteria(Criteria.where("createdBy").is(claims.getUserId()));
        query.addCriteria(notDeletedCriteria());

        if (StringUtils.hasText(type)) {
            query.addCriteria(Criteria.where("type").is(type.toUpperCase()));
        }

        if (isPublished != null) {
            query.addCriteria(Criteria.where("isPublished").is(isPublished));
        }

        if (StringUtils.hasText(search)) {
            query.addCriteria(Criteria.where("title").regex(search, "i"));
        }

        Query countQuery = Query.of(query).limit(-1).skip(-1);

        query
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .skip((long) (safePage - 1) * safeLimit)
            .limit(safeLimit);

        List<Exam> exams = examsMongoTemplate.find(query, Exam.class);
        long totalTests = examsMongoTemplate.count(countQuery, Exam.class);

        List<ExamSummaryResponse> testDtos = exams.stream().map(this::toExamSummary).toList();
        int totalPages = totalTests == 0 ? 0 : (int) Math.ceil((double) totalTests / safeLimit);

        return PagedTestsResponse.builder()
            .tests(testDtos)
            .currentPage(safePage)
            .totalPages(totalPages)
            .totalTests(totalTests)
            .limit(safeLimit)
            .build();
    }

    @Override
    public ExamDetailResponse createTest(HttpServletRequest request, CreateExamRequest requestBody) {
        SuperAdminClaims claims = verifySuperAdmin(request);

        String normalizedType = normalizeType(requestBody.getType());
        validateType(normalizedType);
        validateQuestions(requestBody.getQuestions(), requestBody.getTotalQuestions());

        Exam exam = Exam.builder()
            .title(requestBody.getTitle())
            .type(normalizedType)
            .createdBy(claims.getUserId())
            .durationMinutes(requestBody.getDurationMinutes())
            .totalQuestions(requestBody.getTotalQuestions())
            .totalMarks(requestBody.getTotalMarks())
            .isPublished(Boolean.TRUE.equals(requestBody.getIsPublished()))
            .questions(mapQuestions(requestBody.getQuestions()))
            .isDeleted(false)
            .deletedAt(null)
            .deletedBy(null)
            .createdAt(Instant.now())
            .build();

        if ("SCHEDULED".equals(normalizedType)) {
            validateScheduledExam(requestBody);
            exam.setState(requestBody.getState());
            exam.setCollege(requestBody.getCollege());
            exam.setBranch(requestBody.getBranch());
            exam.setYearOfPassing(requestBody.getYearOfPassing());
            exam.setScheduledWindow(
                ScheduledWindow.builder()
                    .start(requestBody.getScheduledWindow().getStart())
                    .end(requestBody.getScheduledWindow().getEnd())
                    .build()
            );
            exam.setNegativeMarking(0.25);
            exam.setShuffleQuestions(false);
            exam.setShuffleOptions(false);
        } else {
            validatePracticeExam(requestBody);
            exam.setState(null);
            exam.setCollege(null);
            exam.setBranch(null);
            exam.setYearOfPassing(null);
            exam.setScheduledWindow(null);
            exam.setNegativeMarking(0.0);
            exam.setShuffleQuestions(true);
            exam.setShuffleOptions(true);
        }

        Exam savedExam = examRepository.save(exam);

        return ExamDetailResponse.builder()
            .id(savedExam.getId())
            .title(savedExam.getTitle())
            .type(savedExam.getType())
            .isPublished(savedExam.getIsPublished())
            .totalQuestions(savedExam.getTotalQuestions())
            .createdBy(savedExam.getCreatedBy())
            .questions(savedExam.getQuestions().stream().map(this::toQuestionClientResponse).toList())
            .build();
    }

    @Override
    public DeleteTestResponse deleteTest(HttpServletRequest request, String testId) {
        SuperAdminClaims claims = verifySuperAdmin(request);

        Exam exam = examRepository.findById(testId)
            .orElseThrow(() -> new ResourceNotFoundException("No test with this ID"));

        if (Boolean.TRUE.equals(exam.getIsDeleted())) {
            throw new ResourceNotFoundException("No test with this ID");
        }

        if (!claims.getUserId().equals(exam.getCreatedBy())) {
            throw new ForbiddenException("Not created by this admin");
        }

        exam.setIsDeleted(true);
        exam.setDeletedAt(Instant.now());
        exam.setDeletedBy(claims.getUserId());
        examRepository.save(exam);

        return DeleteTestResponse.builder()
            .deletedTestId(exam.getId())
            .title(exam.getTitle())
            .build();
    }

    private SuperAdminClaims verifySuperAdmin(HttpServletRequest request) {
        SuperAdminClaims claims = (SuperAdminClaims) request.getAttribute("superAdminClaims");
        if (
            claims == null
                || !Boolean.TRUE.equals(claims.getNoScopeRestriction())
                || !"FULL".equals(claims.getPlatformAccess())
        ) {
            throw new ForbiddenException("Super Admin access required");
        }
        return claims;
    }

    private Criteria notDeletedCriteria() {
        return new Criteria().orOperator(
            Criteria.where("isDeleted").exists(false),
            Criteria.where("isDeleted").is(false)
        );
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }

    private void validateType(String type) {
        if (!"SCHEDULED".equals(type) && !"PRACTICE".equals(type)) {
            throw new ValidationException("Type must be SCHEDULED or PRACTICE");
        }
    }

    private void validateQuestions(List<CreateQuestionRequest> questions, Integer totalQuestions) {
        if (questions == null || questions.isEmpty()) {
            throw new ValidationException("At least one question is required");
        }
        if (totalQuestions == null || questions.size() != totalQuestions) {
            throw new ValidationException("questions.size() must match totalQuestions");
        }

        for (CreateQuestionRequest question : questions) {
            String difficulty = question.getDifficulty() == null ? "" : question.getDifficulty().toUpperCase();
            if (!"EASY".equals(difficulty) && !"MEDIUM".equals(difficulty) && !"HARD".equals(difficulty)) {
                throw new ValidationException("Question difficulty must be EASY, MEDIUM, or HARD");
            }
        }
    }

    private void validateScheduledExam(CreateExamRequest request) {
        if (
            request.getState() == null
                || request.getCollege() == null
                || request.getBranch() == null
                || request.getYearOfPassing() == null
                || request.getScheduledWindow() == null
        ) {
            throw new ValidationException("SCHEDULED exams require state, college, branch, yearOfPassing, and scheduledWindow");
        }

        if (request.getScheduledWindow().getStart() == null || request.getScheduledWindow().getEnd() == null) {
            throw new ValidationException("Scheduled window start and end are required");
        }

        if (!request.getScheduledWindow().getStart().isAfter(Instant.now())) {
            throw new UnprocessableException("scheduledWindow.start must be in the future");
        }

        if (!request.getScheduledWindow().getEnd().isAfter(request.getScheduledWindow().getStart())) {
            throw new UnprocessableException("scheduledWindow.end must be after scheduledWindow.start");
        }
    }

    private void validatePracticeExam(CreateExamRequest request) {
        if (
            request.getState() != null
                || request.getCollege() != null
                || request.getBranch() != null
                || request.getYearOfPassing() != null
                || request.getScheduledWindow() != null
        ) {
            throw new ValidationException("PRACTICE exams must have null state, college, branch, yearOfPassing, and scheduledWindow");
        }
    }

    private List<Question> mapQuestions(List<CreateQuestionRequest> questionRequests) {
        List<Question> questions = new ArrayList<>();
        for (CreateQuestionRequest questionRequest : questionRequests) {
            questions.add(
                Question.builder()
                    .text(questionRequest.getText())
                    .options(questionRequest.getOptions())
                    .correctIndex(questionRequest.getCorrectIndex())
                    .explanation(questionRequest.getExplanation() == null ? "" : questionRequest.getExplanation())
                    .topic(questionRequest.getTopic())
                    .difficulty(questionRequest.getDifficulty().toUpperCase())
                    .build()
            );
        }
        return questions;
    }

    private ExamSummaryResponse toExamSummary(Exam exam) {
        return ExamSummaryResponse.builder()
            .id(exam.getId())
            .title(exam.getTitle())
            .type(exam.getType())
            .state(exam.getState())
            .college(exam.getCollege())
            .branch(exam.getBranch())
            .yearOfPassing(exam.getYearOfPassing())
            .scheduledWindow(
                exam.getScheduledWindow() == null
                    ? null
                    : ScheduledWindowDto.builder()
                        .start(exam.getScheduledWindow().getStart())
                        .end(exam.getScheduledWindow().getEnd())
                        .build()
            )
            .durationMinutes(exam.getDurationMinutes())
            .totalQuestions(exam.getTotalQuestions())
            .totalMarks(exam.getTotalMarks())
            .negativeMarking(exam.getNegativeMarking())
            .shuffleQuestions(exam.getShuffleQuestions())
            .shuffleOptions(exam.getShuffleOptions())
            .isPublished(exam.getIsPublished())
            .createdBy(exam.getCreatedBy())
            .createdAt(exam.getCreatedAt())
            .build();
    }

    private QuestionClientResponse toQuestionClientResponse(Question question) {
        return QuestionClientResponse.builder()
            .text(question.getText())
            .options(question.getOptions())
            .explanation(question.getExplanation())
            .topic(question.getTopic())
            .difficulty(question.getDifficulty())
            .build();
    }
}
