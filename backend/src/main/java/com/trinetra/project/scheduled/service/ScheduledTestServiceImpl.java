package com.trinetra.project.scheduled.service;

import com.trinetra.project.common.dto.AnswerSubmission;
import com.trinetra.project.common.exception.ConflictException;
import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.exception.GoneException;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.common.exception.TooEarlyException;
import com.trinetra.project.common.exception.UnauthorizedException;
import com.trinetra.project.common.exception.ValidationException;
import com.trinetra.project.common.security.claims.StudentClaims;
import com.trinetra.project.exam.dto.QuestionClientResponse;
import com.trinetra.project.exam.model.Exam;
import com.trinetra.project.exam.model.embedded.Question;
import com.trinetra.project.exam.model.embedded.ScheduledWindow;
import com.trinetra.project.exam.repository.ExamRepository;
import com.trinetra.project.exam.util.QuestionMapper;
import com.trinetra.project.scheduled.dto.request.SubmitTestRequest;
import com.trinetra.project.scheduled.dto.response.ScheduledWindowResponse;
import com.trinetra.project.scheduled.dto.response.TestCurrentQuestionResponse;
import com.trinetra.project.scheduled.dto.response.TestQuestionResponse;
import com.trinetra.project.scheduled.dto.response.TestResultBreakdownResponse;
import com.trinetra.project.scheduled.dto.response.TestResultResponse;
import com.trinetra.project.scheduled.dto.response.TestSubmitResponse;
import com.trinetra.project.student.model.Student;
import com.trinetra.project.student.model.embedded.AptitudeHistory;
import com.trinetra.project.student.repository.StudentRepository;
import com.trinetra.project.student.service.AptitudeHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTestServiceImpl implements ScheduledTestService {

    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final AptitudeHistoryService aptitudeHistoryService;
    private final QuestionMapper questionMapper;

    public ScheduledTestServiceImpl(
        ExamRepository examRepository,
        StudentRepository studentRepository,
        AptitudeHistoryService aptitudeHistoryService,
        QuestionMapper questionMapper
    ) {
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
        this.aptitudeHistoryService = aptitudeHistoryService;
        this.questionMapper = questionMapper;
    }

    @Override
    public TestQuestionResponse getScheduledTestQuestion(HttpServletRequest request, String testId, int questionIndex) {
        StudentClaims claims = verifyApprovedStudent(request);

        Exam exam = findScheduledExamOrThrow(testId);
        ensurePublished(exam);
        ensureEligibility(exam, claims);
        ensureWithinWindow(exam.getScheduledWindow());

        List<Question> orderedQuestions = getOrderedQuestions(exam, claims.getUserId());
        if (questionIndex < 0 || questionIndex >= orderedQuestions.size()) {
            throw new ValidationException("Invalid questionIndex");
        }

        Question question = orderedQuestions.get(questionIndex);
        List<Integer> optionOrder = getOptionOrder(exam, question, claims.getUserId(), questionIndex);

        // SECURITY: correctIndex intentionally excluded.
        QuestionClientResponse baseQuestion = questionMapper.toClientResponse(question, questionIndex);

        return TestQuestionResponse.builder()
            .testId(exam.getId())
            .title(exam.getTitle())
            .type(exam.getType())
            .totalQuestions(orderedQuestions.size())
            .totalMarks(safeInt(exam.getTotalMarks(), orderedQuestions.size()))
            .negativeMarking(safeDouble(exam.getNegativeMarking(), 0.25))
            .durationMinutes(exam.getDurationMinutes())
            .scheduledWindow(
                ScheduledWindowResponse.builder()
                    .start(exam.getScheduledWindow() == null ? null : exam.getScheduledWindow().getStart())
                    .end(exam.getScheduledWindow() == null ? null : exam.getScheduledWindow().getEnd())
                    .build()
            )
            .currentQuestion(
                TestCurrentQuestionResponse.builder()
                    .index(questionIndex)
                    .text(baseQuestion.getText())
                    .options(mapOptions(question, optionOrder))
                    .topic(baseQuestion.getTopic())
                    .difficulty(baseQuestion.getDifficulty())
                    .build()
            )
            .hasNext(questionIndex < orderedQuestions.size() - 1)
            .build();
    }

    @Override
    public TestSubmitResponse submitScheduledTest(HttpServletRequest request, String testId, SubmitTestRequest requestBody) {
        StudentClaims claims = verifyApprovedStudent(request);

        Exam exam = findScheduledExamOrThrow(testId);
        ensurePublished(exam);
        ensureEligibility(exam, claims);
        ensureWithinWindow(exam.getScheduledWindow());

        Student student = findStudentOrThrow(claims.getUserId());
        ensureNotAlreadySubmitted(student, testId);

        List<Question> orderedQuestions = getOrderedQuestions(exam, claims.getUserId());
        int totalQuestions = orderedQuestions.size();
        if (requestBody.getAnswers() == null || requestBody.getAnswers().size() != totalQuestions) {
            throw new ValidationException("answers size must match totalQuestions");
        }

        Map<Integer, AnswerSubmission> answerMap = toAnswerMap(requestBody.getAnswers(), totalQuestions);

        int correct = 0;
        int wrong = 0;
        int skipped = 0;

        for (int i = 0; i < totalQuestions; i++) {
            AnswerSubmission answer = answerMap.get(i);
            if (answer == null) {
                throw new ValidationException("answers must include all questionIndex values");
            }

            Question question = orderedQuestions.get(i);
            Integer selectedOption = answer.getSelectedOption();

            if (selectedOption == null) {
                skipped++;
                continue;
            }

            List<Integer> optionOrder = getOptionOrder(exam, question, claims.getUserId(), i);
            if (selectedOption < 0 || selectedOption >= optionOrder.size()) {
                throw new ValidationException("Invalid selectedOption for questionIndex " + i);
            }

            int originalOptionIndex = optionOrder.get(selectedOption);
            if (question.getCorrectIndex() != null && originalOptionIndex == question.getCorrectIndex()) {
                correct++;
            } else {
                wrong++;
            }
        }

        int attempted = correct + wrong;
        double negativeMarking = safeDouble(exam.getNegativeMarking(), 0.25);
        double score = correct - (wrong * negativeMarking);
        score = Math.max(0.0, score);

        int totalMarks = safeInt(exam.getTotalMarks(), totalQuestions);
        double percentage = totalMarks > 0 ? (score / totalMarks) * 100.0 : 0.0;
        // TODO: Capture server-side time tracking per scheduled test session.
        int timeTaken = requestBody.getTimeTaken() == null ? 0 : requestBody.getTimeTaken();

        AptitudeHistory historyEntry = AptitudeHistory.builder()
            .topic(exam.getTitle())
            .score(score)
            .timeTaken(timeTaken)
            .type("SCHEDULED")
            .examId(testId)
            .attemptedAt(Instant.now())
            .attempted(attempted)
            .correct(correct)
            .wrong(wrong)
            .skipped(skipped)
            .build();

        aptitudeHistoryService.appendToHistory(claims.getUserId(), historyEntry);

        return TestSubmitResponse.builder()
            .testId(exam.getId())
            .title(exam.getTitle())
            .type(exam.getType())
            .totalQuestions(totalQuestions)
            .attempted(attempted)
            .correct(correct)
            .wrong(wrong)
            .skipped(skipped)
            .score(score)
            .totalMarks(totalMarks)
            .negativeMarking(negativeMarking)
            .timeTaken(timeTaken)
            .percentage(percentage)
            .build();
    }

    @Override
    public TestResultResponse getScheduledTestResult(HttpServletRequest request, String testId) {
        StudentClaims claims = verifyApprovedStudent(request);

        Exam exam = findScheduledExamOrThrow(testId);
        ensurePublished(exam);
        ensureEligibility(exam, claims);

        Student student = findStudentOrThrow(claims.getUserId());
        AptitudeHistory history = (student.getAptitudeHistory() == null ? new ArrayList<AptitudeHistory>() : student.getAptitudeHistory())
            .stream()
            .filter(entry -> "SCHEDULED".equalsIgnoreCase(entry.getType()))
            .filter(entry -> testId.equals(entry.getExamId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("You have not attempted this test"));

        int totalMarks = safeInt(exam.getTotalMarks(), 0);
        double negativeMarking = safeDouble(exam.getNegativeMarking(), 0.25);
        double percentage = totalMarks > 0 && history.getScore() != null ? (history.getScore() / totalMarks) * 100.0 : 0.0;

        return TestResultResponse.builder()
            .testId(testId)
            .title(exam.getTitle())
            .type(exam.getType())
            .score(history.getScore())
            .totalMarks(totalMarks)
            .negativeMarking(negativeMarking)
            .timeTaken(history.getTimeTaken())
            .percentage(percentage)
            .attemptedAt(history.getAttemptedAt())
            .breakdown(
                TestResultBreakdownResponse.builder()
                    .attempted(safeInt(history.getAttempted(), 0))
                    .correct(safeInt(history.getCorrect(), 0))
                    .wrong(safeInt(history.getWrong(), 0))
                    .skipped(safeInt(history.getSkipped(), 0))
                    .build()
            )
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

    private Exam findScheduledExamOrThrow(String testId) {
        Exam exam = examRepository.findById(testId)
            .orElseThrow(() -> new ResourceNotFoundException("No exam with this testId"));

        if (Boolean.TRUE.equals(exam.getIsDeleted())) {
            throw new ResourceNotFoundException("No exam with this testId");
        }

        return exam;
    }

    private void ensurePublished(Exam exam) {
        if (!Boolean.TRUE.equals(exam.getIsPublished())) {
            throw new ForbiddenException("Exam is not published");
        }
    }

    private void ensureEligibility(Exam exam, StudentClaims claims) {
        boolean eligible = safeEquals(exam.getState(), claims.getState())
            && safeEquals(exam.getCollege(), claims.getCollege())
            && safeEquals(exam.getBranch(), claims.getBranch())
            && exam.getYearOfPassing() != null
            && exam.getYearOfPassing() == claims.getYearOfPassing();

        if (!eligible) {
            throw new ForbiddenException("You are not eligible for this exam");
        }
    }

    private void ensureWithinWindow(ScheduledWindow window) {
        if (window == null || window.getStart() == null || window.getEnd() == null) {
            throw new ForbiddenException("Scheduled window is not configured");
        }

        Instant now = Instant.now();
        if (now.isBefore(window.getStart())) {
            throw new TooEarlyException("Exam has not started yet");
        }
        if (now.isAfter(window.getEnd())) {
            throw new GoneException("Exam time window has passed");
        }
    }

    private List<Question> getOrderedQuestions(Exam exam, String studentId) {
        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            throw new ResourceNotFoundException("No questions configured for this exam");
        }

        List<Question> ordered = new ArrayList<>(exam.getQuestions());
        if (Boolean.TRUE.equals(exam.getShuffleQuestions())) {
            Collections.shuffle(ordered, new Random(studentId.hashCode()));
        }
        return ordered;
    }

    private List<Integer> getOptionOrder(Exam exam, Question question, String studentId, int questionIndex) {
        List<String> options = question.getOptions() == null ? new ArrayList<>() : question.getOptions();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            order.add(i);
        }

        if (Boolean.TRUE.equals(exam.getShuffleOptions())) {
            long seed = 31L * studentId.hashCode() + 17L * exam.getId().hashCode() + questionIndex;
            Collections.shuffle(order, new Random(seed));
        }

        return order;
    }

    private List<String> mapOptions(Question question, List<Integer> order) {
        List<String> options = question.getOptions() == null ? new ArrayList<>() : question.getOptions();
        List<String> shuffled = new ArrayList<>();
        for (Integer index : order) {
            shuffled.add(options.get(index));
        }
        return shuffled;
    }

    private Map<Integer, AnswerSubmission> toAnswerMap(List<AnswerSubmission> answers, int totalQuestions) {
        Map<Integer, AnswerSubmission> answerMap = new HashMap<>();

        for (AnswerSubmission answer : answers) {
            if (answer.getQuestionIndex() == null || answer.getQuestionIndex() < 0 || answer.getQuestionIndex() >= totalQuestions) {
                throw new ValidationException("Invalid questionIndex in answers");
            }

            if (answerMap.containsKey(answer.getQuestionIndex())) {
                throw new ValidationException("Duplicate questionIndex in answers");
            }

            answerMap.put(answer.getQuestionIndex(), answer);
        }

        return answerMap;
    }

    private Student findStudentOrThrow(String studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("No student with this ID"));

        if (!"STUDENT".equals(student.getRole()) || Boolean.TRUE.equals(student.getIsDeleted())) {
            throw new ResourceNotFoundException("No student with this ID");
        }

        return student;
    }

    private void ensureNotAlreadySubmitted(Student student, String testId) {
        List<AptitudeHistory> history = student.getAptitudeHistory() == null ? new ArrayList<>() : student.getAptitudeHistory();
        boolean alreadySubmitted = history.stream().anyMatch(entry ->
            "SCHEDULED".equalsIgnoreCase(entry.getType()) && testId.equals(entry.getExamId())
        );

        if (alreadySubmitted) {
            throw new ConflictException("You have already submitted this test");
        }
    }

    private boolean safeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private double safeDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
