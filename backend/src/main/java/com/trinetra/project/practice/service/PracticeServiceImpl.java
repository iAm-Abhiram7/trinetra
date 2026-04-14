package com.trinetra.project.practice.service;

import com.trinetra.project.common.dto.AnswerSubmission;
import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.common.exception.UnauthorizedException;
import com.trinetra.project.common.exception.ValidationException;
import com.trinetra.project.common.security.claims.StudentClaims;
import com.trinetra.project.exam.dto.QuestionClientResponse;
import com.trinetra.project.exam.model.Exam;
import com.trinetra.project.exam.model.embedded.Question;
import com.trinetra.project.exam.repository.ExamRepository;
import com.trinetra.project.exam.util.QuestionMapper;
import com.trinetra.project.practice.dto.request.SubmitExamRequest;
import com.trinetra.project.practice.dto.response.ExamCurrentQuestionResponse;
import com.trinetra.project.practice.dto.response.ExamQuestionResponse;
import com.trinetra.project.practice.dto.response.ExamResultResponse;
import com.trinetra.project.practice.dto.response.ExamSubmitResponse;
import com.trinetra.project.practice.dto.response.PracticeAttemptResponse;
import com.trinetra.project.practice.dto.response.PracticePaginationResponse;
import com.trinetra.project.student.model.Student;
import com.trinetra.project.student.model.embedded.AptitudeHistory;
import com.trinetra.project.student.repository.StudentRepository;
import com.trinetra.project.student.service.AptitudeHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class PracticeServiceImpl implements PracticeService {

    private final MongoTemplate examsMongoTemplate;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final AptitudeHistoryService aptitudeHistoryService;
    private final QuestionMapper questionMapper;

    public PracticeServiceImpl(
        @Qualifier("examsMongoTemplate") MongoTemplate examsMongoTemplate,
        ExamRepository examRepository,
        StudentRepository studentRepository,
        AptitudeHistoryService aptitudeHistoryService,
        QuestionMapper questionMapper
    ) {
        this.examsMongoTemplate = examsMongoTemplate;
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
        this.aptitudeHistoryService = aptitudeHistoryService;
        this.questionMapper = questionMapper;
    }

    public ExamQuestionResponse getPracticeExamQuestion(HttpServletRequest request, String topic, int questionIndex) {
        StudentClaims claims = verifyApprovedStudent(request);

        Exam exam = findLatestPracticeExamByTopic(topic);
        List<Question> orderedQuestions = getOrderedQuestions(exam, claims.getUserId());

        if (questionIndex < 0 || questionIndex >= orderedQuestions.size()) {
            throw new ValidationException("Invalid questionIndex");
        }

        Question question = orderedQuestions.get(questionIndex);
        List<Integer> optionOrder = getOptionOrder(exam, question, claims.getUserId(), questionIndex);

        // SECURITY: correctIndex intentionally excluded.
        QuestionClientResponse baseQuestion = questionMapper.toClientResponse(question, questionIndex);

        return ExamQuestionResponse.builder()
            .examId(exam.getId())
            .title(exam.getTitle())
            .type("PRACTICE")
            .totalQuestions(orderedQuestions.size())
            .totalMarks(safeInt(exam.getTotalMarks(), orderedQuestions.size()))
            .negativeMarking(safeDouble(exam.getNegativeMarking(), 0.0))
            .durationMinutes(exam.getDurationMinutes())
            .currentQuestion(
                ExamCurrentQuestionResponse.builder()
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
    public ExamSubmitResponse submitPracticeExam(HttpServletRequest request, String topic, SubmitExamRequest requestBody) {
        StudentClaims claims = verifyApprovedStudent(request);

        Exam exam = examRepository.findById(requestBody.getExamId())
            .orElseThrow(() -> new ResourceNotFoundException("No practice exam for this topic"));

        if (
            !"PRACTICE".equals(exam.getType())
                || !Boolean.TRUE.equals(exam.getIsPublished())
                || Boolean.TRUE.equals(exam.getIsDeleted())
                || !examContainsTopic(exam, topic)
        ) {
            throw new ResourceNotFoundException("No practice exam for this topic");
        }

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
        double score = correct;
        int totalMarks = safeInt(exam.getTotalMarks(), totalQuestions);
        double percentage = totalMarks > 0 ? (score / totalMarks) * 100.0 : 0.0;
        // TODO: Capture server-side time tracking per exam session.
        int timeTaken = requestBody.getTimeTaken() == null ? 0 : requestBody.getTimeTaken();

        AptitudeHistory historyEntry = AptitudeHistory.builder()
            .topic(normalizeTopic(topic))
            .score(score)
            .timeTaken(timeTaken)
            .type("PRACTICE")
            .examId(exam.getId())
            .attemptedAt(Instant.now())
            .attempted(attempted)
            .correct(correct)
            .wrong(wrong)
            .skipped(skipped)
            .build();

        aptitudeHistoryService.appendToHistory(claims.getUserId(), historyEntry);

        return ExamSubmitResponse.builder()
            .examId(exam.getId())
            .topic(normalizeTopic(topic))
            .type("PRACTICE")
            .totalQuestions(totalQuestions)
            .attempted(attempted)
            .correct(correct)
            .wrong(wrong)
            .skipped(skipped)
            .score(score)
            .totalMarks(totalMarks)
            .timeTaken(timeTaken)
            .percentage(percentage)
            .build();
    }

    @Override
    public ExamResultResponse getPracticeResults(HttpServletRequest request, String topic, int page, int limit, boolean all) {
        StudentClaims claims = verifyApprovedStudent(request);
        Student student = findStudentOrThrow(claims.getUserId());

        String normalizedTopic = normalizeTopic(topic);
        List<AptitudeHistory> practiceHistory = (student.getAptitudeHistory() == null ? new ArrayList<AptitudeHistory>() : student.getAptitudeHistory())
            .stream()
            .filter(entry -> "PRACTICE".equalsIgnoreCase(entry.getType()))
            .filter(entry -> normalizedTopic.equals(normalizeTopic(entry.getTopic())))
            .sorted(Comparator.comparing(AptitudeHistory::getAttemptedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .toList();

        if (practiceHistory.isEmpty()) {
            throw new ResourceNotFoundException("No attempts found for this topic");
        }

        if (!all) {
            AptitudeHistory latest = practiceHistory.get(0);
            int totalMarks = resolveTotalMarks(latest.getExamId());
            double percentage = totalMarks > 0 && latest.getScore() != null ? (latest.getScore() / totalMarks) * 100.0 : 0.0;

            return ExamResultResponse.builder()
                .topic(normalizedTopic)
                .score(latest.getScore())
                .totalMarks(totalMarks)
                .timeTaken(latest.getTimeTaken())
                .type(latest.getType())
                .attemptedAt(latest.getAttemptedAt())
                .percentage(percentage)
                .attempts(null)
                .pagination(null)
                .build();
        }

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 10 : limit;
        int totalAttempts = practiceHistory.size();
        int totalPages = totalAttempts == 0 ? 0 : (int) Math.ceil((double) totalAttempts / safeLimit);

        int fromIndex = Math.min((safePage - 1) * safeLimit, totalAttempts);
        int toIndex = Math.min(fromIndex + safeLimit, totalAttempts);

        List<PracticeAttemptResponse> attempts = practiceHistory.subList(fromIndex, toIndex)
            .stream()
            .map(entry ->
                PracticeAttemptResponse.builder()
                    .score(entry.getScore())
                    .timeTaken(entry.getTimeTaken())
                    .attemptedAt(entry.getAttemptedAt())
                    .build()
            )
            .toList();

        return ExamResultResponse.builder()
            .topic(normalizedTopic)
            .attempts(attempts)
            .pagination(
                PracticePaginationResponse.builder()
                    .currentPage(safePage)
                    .totalPages(totalPages)
                    .totalAttempts((long) totalAttempts)
                    .limit(safeLimit)
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

    private Exam findLatestPracticeExamByTopic(String topic) {
        String normalizedTopic = normalizeTopic(topic);

        Query query = new Query();
        query.addCriteria(Criteria.where("type").is("PRACTICE"));
        query.addCriteria(Criteria.where("isPublished").is(true));
        query.addCriteria(
            new Criteria().orOperator(
                Criteria.where("isDeleted").exists(false),
                Criteria.where("isDeleted").is(false)
            )
        );
        query.addCriteria(
            Criteria.where("questions").elemMatch(
                Criteria.where("topic").regex("^" + Pattern.quote(normalizedTopic) + "$", "i")
            )
        );
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        query.limit(1);

        Exam exam = examsMongoTemplate.findOne(query, Exam.class);
        if (exam == null) {
            throw new ResourceNotFoundException("No practice exam for this topic");
        }
        return exam;
    }

    private boolean examContainsTopic(Exam exam, String topic) {
        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            return false;
        }

        String normalizedTopic = normalizeTopic(topic);
        return exam.getQuestions().stream().anyMatch(question -> normalizedTopic.equals(normalizeTopic(question.getTopic())));
    }

    private List<Question> getOrderedQuestions(Exam exam, String studentId) {
        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            throw new ResourceNotFoundException("No practice exam for this topic");
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

    private int resolveTotalMarks(String examId) {
        if (examId == null) {
            return 0;
        }
        return examRepository.findById(examId)
            .filter(exam -> !Boolean.TRUE.equals(exam.getIsDeleted()))
            .map(Exam::getTotalMarks)
            .filter(totalMarks -> totalMarks != null && totalMarks > 0)
            .orElse(0);
    }

    private String normalizeTopic(String topic) {
        if (topic == null) {
            return "";
        }
        return topic.toLowerCase().trim().replaceAll("\\s+", "-");
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private double safeDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
