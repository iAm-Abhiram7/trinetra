package com.trinetra.project.admin.service;

import com.trinetra.project.admin.dto.request.BulkApproveRequest;
import com.trinetra.project.admin.dto.request.BulkRejectRequest;
import com.trinetra.project.admin.dto.request.CompareStudentsRequest;
import com.trinetra.project.admin.dto.request.RejectStudentRequest;
import com.trinetra.project.admin.dto.response.AdminHistoryItemResponse;
import com.trinetra.project.admin.dto.response.AdminStudentDetailResponse;
import com.trinetra.project.admin.dto.response.ApprovalStatusResponse;
import com.trinetra.project.admin.dto.response.ApprovalStudentResponse;
import com.trinetra.project.admin.dto.response.BulkApproveResponse;
import com.trinetra.project.admin.dto.response.BulkRejectResponse;
import com.trinetra.project.admin.dto.response.HistoryActionResponse;
import com.trinetra.project.admin.dto.response.PagedAdminHistoryResponse;
import com.trinetra.project.admin.dto.response.PagedApprovalStudentsResponse;
import com.trinetra.project.admin.dto.response.PagedTestStatusResponse;
import com.trinetra.project.admin.dto.response.StudentApprovalActionResponse;
import com.trinetra.project.admin.dto.response.StudentCompareItemResponse;
import com.trinetra.project.admin.dto.response.StudentCompareResponse;
import com.trinetra.project.admin.dto.response.StudentCompareStatsResponse;
import com.trinetra.project.admin.dto.response.StudentHistoryResponse;
import com.trinetra.project.admin.dto.response.TestStatusRowResponse;
import com.trinetra.project.admin.model.CollegeAdmin;
import com.trinetra.project.admin.model.embedded.Scope;
import com.trinetra.project.admin.repository.CollegeAdminRepository;
import com.trinetra.project.common.exception.ConflictException;
import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.common.exception.ValidationException;
import com.trinetra.project.common.security.claims.AdminClaims;
import com.trinetra.project.common.security.claims.SuperAdminClaims;
import com.trinetra.project.exam.model.Exam;
import com.trinetra.project.exam.repository.ExamRepository;
import com.trinetra.project.student.model.Student;
import com.trinetra.project.student.model.embedded.AptitudeHistory;
import com.trinetra.project.student.model.embedded.ApprovalStatus;
import com.trinetra.project.student.repository.StudentRepository;
import com.trinetra.project.superadmin.dto.response.ScopeDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private final MongoTemplate usersMongoTemplate;
    private final StudentRepository studentRepository;
    private final CollegeAdminRepository collegeAdminRepository;
    private final ExamRepository examRepository;

    public AdminManagementServiceImpl(
        @Qualifier("usersMongoTemplate") MongoTemplate usersMongoTemplate,
        StudentRepository studentRepository,
        CollegeAdminRepository collegeAdminRepository,
        ExamRepository examRepository
    ) {
        this.usersMongoTemplate = usersMongoTemplate;
        this.studentRepository = studentRepository;
        this.collegeAdminRepository = collegeAdminRepository;
        this.examRepository = examRepository;
    }

    @Override
    public PagedApprovalStudentsResponse getPendingStudents(
        HttpServletRequest request,
        int page,
        int limit,
        String branch,
        String search
    ) {
        AdminActor actor = verifyAdminActor(request);

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 20 : limit;

        Query query = new Query();
        query.addCriteria(Criteria.where("role").is("STUDENT"));
        query.addCriteria(notDeletedCriteria());
        query.addCriteria(Criteria.where("approvalStatus.status").is("PENDING"));

        Criteria scopeCriteria = buildScopedStudentCriteria(actor, branch);
        if (scopeCriteria != null) {
            query.addCriteria(scopeCriteria);
        }

        if (StringUtils.hasText(search)) {
            query.addCriteria(
                new Criteria().orOperator(
                    Criteria.where("name").regex(search, "i"),
                    Criteria.where("email").regex(search, "i")
                )
            );
        }

        Query countQuery = Query.of(query).limit(-1).skip(-1);

        query
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .skip((long) (safePage - 1) * safeLimit)
            .limit(safeLimit);

        List<Student> students = usersMongoTemplate.find(query, Student.class);
        long totalStudents = usersMongoTemplate.count(countQuery, Student.class);
        int totalPages = totalStudents == 0 ? 0 : (int) Math.ceil((double) totalStudents / safeLimit);

        return PagedApprovalStudentsResponse.builder()
            .students(students.stream().map(this::toApprovalStudentResponse).toList())
            .currentPage(safePage)
            .totalPages(totalPages)
            .totalStudents(totalStudents)
            .limit(safeLimit)
            .build();
    }

    @Override
    public PagedApprovalStudentsResponse getRejectedStudents(
        HttpServletRequest request,
        int page,
        int limit,
        String branch,
        String search
    ) {
        AdminActor actor = verifyAdminActor(request);

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 20 : limit;

        Query query = new Query();
        query.addCriteria(Criteria.where("role").is("STUDENT"));
        query.addCriteria(notDeletedCriteria());
        query.addCriteria(Criteria.where("approvalStatus.status").is("REJECTED"));
        query.addCriteria(Criteria.where("approvalStatus.approvedBy").is(actor.userId()));

        Criteria scopeCriteria = buildScopedStudentCriteria(actor, branch);
        if (scopeCriteria != null) {
            query.addCriteria(scopeCriteria);
        }

        if (StringUtils.hasText(search)) {
            query.addCriteria(
                new Criteria().orOperator(
                    Criteria.where("name").regex(search, "i"),
                    Criteria.where("email").regex(search, "i")
                )
            );
        }

        Query countQuery = Query.of(query).limit(-1).skip(-1);

        query
            .with(Sort.by(Sort.Direction.DESC, "approvalStatus.approvedAt"))
            .skip((long) (safePage - 1) * safeLimit)
            .limit(safeLimit);

        List<Student> students = usersMongoTemplate.find(query, Student.class);
        long totalStudents = usersMongoTemplate.count(countQuery, Student.class);
        int totalPages = totalStudents == 0 ? 0 : (int) Math.ceil((double) totalStudents / safeLimit);

        return PagedApprovalStudentsResponse.builder()
            .students(students.stream().map(this::toApprovalStudentResponse).toList())
            .currentPage(safePage)
            .totalPages(totalPages)
            .totalStudents(totalStudents)
            .limit(safeLimit)
            .build();
    }

    @Override
    public StudentApprovalActionResponse approveStudent(HttpServletRequest request, String studentId) {
        AdminActor actor = verifyAdminActor(request);
        Student student = findStudentOrThrow(studentId);
        ensureStudentInScope(actor, student);
        ensurePending(student);

        Instant now = Instant.now();
        ApprovalStatus status = student.getApprovalStatus();
        status.setStatus("APPROVED");
        status.setApprovedBy(actor.userId());
        status.setApprovedAt(now);
        status.setRejectionReason(null);
        student.setApprovalStatus(status);

        Student savedStudent = studentRepository.save(student);
        return toStudentApprovalActionResponse(savedStudent);
    }

    @Override
    public BulkApproveResponse approveAllStudents(HttpServletRequest request, BulkApproveRequest requestBody) {
        AdminActor actor = verifyAdminActor(request);
        String branch = requestBody == null ? null : requestBody.getBranch();

        Query query = basePendingQuery(actor, branch);
        long totalPending = usersMongoTemplate.count(Query.of(query).limit(-1).skip(-1), Student.class);
        if (totalPending == 0) {
            throw new ResourceNotFoundException("No pending students");
        }

        Update update = new Update()
            .set("approvalStatus.status", "APPROVED")
            .set("approvalStatus.approvedBy", actor.userId())
            .set("approvalStatus.approvedAt", Instant.now())
            .set("approvalStatus.rejectionReason", null);

        long modifiedCount = usersMongoTemplate.updateMulti(query, update, Student.class).getModifiedCount();

        return BulkApproveResponse.builder()
            .approvedCount(modifiedCount)
            .scope(toScopeDtos(actor.scopes()))
            .build();
    }

    @Override
    public StudentApprovalActionResponse rejectStudent(
        HttpServletRequest request,
        String studentId,
        RejectStudentRequest requestBody
    ) {
        AdminActor actor = verifyAdminActor(request);
        Student student = findStudentOrThrow(studentId);
        ensureStudentInScope(actor, student);
        ensurePending(student);

        String rejectionReason = requestBody.getRejectionReason().trim();

        Instant now = Instant.now();
        ApprovalStatus status = student.getApprovalStatus();
        status.setStatus("REJECTED");
        status.setApprovedBy(actor.userId());
        status.setApprovedAt(now);
        status.setRejectionReason(rejectionReason);
        student.setApprovalStatus(status);

        Student savedStudent = studentRepository.save(student);
        return toStudentApprovalActionResponse(savedStudent);
    }

    @Override
    public BulkRejectResponse rejectAllStudents(HttpServletRequest request, BulkRejectRequest requestBody) {
        AdminActor actor = verifyAdminActor(request);
        String branch = requestBody.getBranch();

        Query query = basePendingQuery(actor, branch);
        long totalPending = usersMongoTemplate.count(Query.of(query).limit(-1).skip(-1), Student.class);
        if (totalPending == 0) {
            throw new ResourceNotFoundException("No pending students");
        }

        Update update = new Update()
            .set("approvalStatus.status", "REJECTED")
            .set("approvalStatus.approvedBy", actor.userId())
            .set("approvalStatus.approvedAt", Instant.now())
            .set("approvalStatus.rejectionReason", requestBody.getRejectionReason().trim());

        long modifiedCount = usersMongoTemplate.updateMulti(query, update, Student.class).getModifiedCount();

        return BulkRejectResponse.builder()
            .rejectedCount(modifiedCount)
            .rejectionReason(requestBody.getRejectionReason().trim())
            .build();
    }

    @Override
    public AdminStudentDetailResponse getStudentDetail(HttpServletRequest request, String studentId) {
        AdminActor actor = verifyAdminActor(request);
        Student student = findStudentOrThrow(studentId);
        ensureStudentInScope(actor, student);
        return toAdminStudentDetailResponse(student);
    }

    @Override
    public StudentCompareResponse compareStudents(HttpServletRequest request, CompareStudentsRequest requestBody) {
        AdminActor actor = verifyAdminActor(request);

        String studentId1 = requestBody.getStudentId1();
        String studentId2 = requestBody.getStudentId2();

        if (studentId1.trim().equals(studentId2.trim())) {
            throw new ValidationException("studentId1 and studentId2 must be different");
        }

        Student student1 = findStudentOrThrow(studentId1);
        Student student2 = findStudentOrThrow(studentId2);

        ensureStudentInScope(actor, student1);
        ensureStudentInScope(actor, student2);

        return StudentCompareResponse.builder()
            .student1(toStudentCompareItemResponse(student1))
            .student2(toStudentCompareItemResponse(student2))
            .build();
    }

    @Override
    public PagedTestStatusResponse getTestStatus(
        HttpServletRequest request,
        String testId,
        int page,
        int limit,
        String sortBy,
        String order
    ) {
        AdminActor actor = verifyAdminActor(request);
        Exam exam = findExamOrThrow(testId);
        ensureExamInScope(actor, exam);

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 20 : limit;

        String sortField = resolveSortField(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        List<Criteria> baseCriteriaList = new ArrayList<>();
        baseCriteriaList.add(Criteria.where("role").is("STUDENT"));
        baseCriteriaList.add(notDeletedCriteria());

        Criteria scopeCriteria = buildScopedStudentCriteria(actor, null);
        if (scopeCriteria != null) {
            baseCriteriaList.add(scopeCriteria);
        }

        Criteria baseCriteria = new Criteria().andOperator(baseCriteriaList.toArray(new Criteria[0]));
        Criteria attemptCriteria = Criteria.where("aptitudeHistory.examId").is(testId);

        Aggregation countAggregation = Aggregation.newAggregation(
            Aggregation.match(baseCriteria),
            Aggregation.unwind("aptitudeHistory"),
            Aggregation.match(attemptCriteria),
            Aggregation.count().as("total")
        );

        Document countDocument = usersMongoTemplate
            .aggregate(countAggregation, "users", Document.class)
            .getUniqueMappedResult();
        long totalStudents = numberToLong(countDocument == null ? null : countDocument.get("total"));

        List<AggregationOperation> rowOperations = new ArrayList<>();
        rowOperations.add(Aggregation.match(baseCriteria));
        rowOperations.add(Aggregation.unwind("aptitudeHistory"));
        rowOperations.add(Aggregation.match(attemptCriteria));
        rowOperations.add(Aggregation.sort(direction, sortField));
        rowOperations.add(Aggregation.skip((long) (safePage - 1) * safeLimit));
        rowOperations.add(Aggregation.limit(safeLimit));

        Aggregation rowAggregation = Aggregation.newAggregation(rowOperations);
        AggregationResults<Document> rowResults = usersMongoTemplate.aggregate(rowAggregation, "users", Document.class);

        List<TestStatusRowResponse> rows = new ArrayList<>();
        for (Document row : rowResults.getMappedResults()) {
            Document attempt = (Document) row.get("aptitudeHistory");
            Double score = numberToNullableDouble(attempt == null ? null : attempt.get("score"));
            Integer timeTaken = numberToNullableInteger(attempt == null ? null : attempt.get("timeTaken"));

            double percentage = 0.0;
            if (score != null && exam.getTotalMarks() != null && exam.getTotalMarks() > 0) {
                percentage = (score / exam.getTotalMarks()) * 100.0;
            }

            rows.add(
                TestStatusRowResponse.builder()
                    .studentId(String.valueOf(row.get("_id")))
                    .name(row.getString("name"))
                    .branch(row.getString("branch"))
                    .score(score)
                    .timeTaken(timeTaken)
                    .percentage(percentage)
                    .attemptedAt(toInstant(attempt == null ? null : attempt.get("attemptedAt")))
                    .build()
            );
        }

        int totalPages = totalStudents == 0 ? 0 : (int) Math.ceil((double) totalStudents / safeLimit);

        return PagedTestStatusResponse.builder()
            .testId(exam.getId())
            .title(exam.getTitle())
            .totalStudentsAttempted(totalStudents)
            .results(rows)
            .currentPage(safePage)
            .totalPages(totalPages)
            .totalStudents(totalStudents)
            .limit(safeLimit)
            .build();
    }

    @Override
    public PagedAdminHistoryResponse getHistory(
        HttpServletRequest request,
        int page,
        int limit,
        String action,
        String branch,
        Instant from,
        Instant to
    ) {
        AdminActor actor = verifyAdminActor(request);

        if (from != null && to != null && to.isBefore(from)) {
            throw new ValidationException("to must be greater than or equal to from");
        }

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 20 : limit;

        Query query = new Query();
        query.addCriteria(Criteria.where("role").is("STUDENT"));
        query.addCriteria(notDeletedCriteria());
        query.addCriteria(Criteria.where("approvalStatus.approvedBy").is(actor.userId()));
        query.addCriteria(Criteria.where("approvalStatus.approvedAt").ne(null));

        if (StringUtils.hasText(action)) {
            String normalizedAction = action.trim().toUpperCase();
            if (!"APPROVED".equals(normalizedAction) && !"REJECTED".equals(normalizedAction)) {
                throw new ValidationException("action must be APPROVED or REJECTED");
            }
            query.addCriteria(Criteria.where("approvalStatus.status").is(normalizedAction));
        }

        Criteria scopeCriteria = buildScopedStudentCriteria(actor, branch);
        if (scopeCriteria != null) {
            query.addCriteria(scopeCriteria);
        }

        if (from != null || to != null) {
            Criteria timeCriteria = Criteria.where("approvalStatus.approvedAt");
            if (from != null) {
                timeCriteria = timeCriteria.gte(from);
            }
            if (to != null) {
                timeCriteria = timeCriteria.lte(to);
            }
            query.addCriteria(timeCriteria);
        }

        Query countQuery = Query.of(query).limit(-1).skip(-1);

        query
            .with(Sort.by(Sort.Direction.DESC, "approvalStatus.approvedAt"))
            .skip((long) (safePage - 1) * safeLimit)
            .limit(safeLimit);

        List<Student> students = usersMongoTemplate.find(query, Student.class);
        long totalRecords = usersMongoTemplate.count(countQuery, Student.class);
        int totalPages = totalRecords == 0 ? 0 : (int) Math.ceil((double) totalRecords / safeLimit);

        return PagedAdminHistoryResponse.builder()
            .history(students.stream().map(this::toAdminHistoryItemResponse).toList())
            .currentPage(safePage)
            .totalPages(totalPages)
            .totalRecords(totalRecords)
            .limit(safeLimit)
            .build();
    }

    @Override
    public StudentHistoryResponse getStudentHistory(HttpServletRequest request, String studentId) {
        AdminActor actor = verifyAdminActor(request);
        Student student = findStudentOrThrow(studentId);
        ensureStudentInScope(actor, student);

        ApprovalStatus approvalStatus = student.getApprovalStatus();
        if (
            approvalStatus == null
                || approvalStatus.getApprovedAt() == null
                || approvalStatus.getApprovedBy() == null
                || !actor.userId().equals(approvalStatus.getApprovedBy())
        ) {
            throw new ResourceNotFoundException("No history found for this student");
        }

        HistoryActionResponse action = HistoryActionResponse.builder()
            .action(approvalStatus.getStatus())
            .actionAt(approvalStatus.getApprovedAt())
            .rejectionReason(approvalStatus.getRejectionReason())
            .build();

        return StudentHistoryResponse.builder()
            .studentId(student.getId())
            .studentName(student.getName())
            .college(student.getCollege())
            .branch(student.getBranch())
            .actions(List.of(action))
            .build();
    }

    private Query basePendingQuery(AdminActor actor, String branch) {
        Query query = new Query();
        query.addCriteria(Criteria.where("role").is("STUDENT"));
        query.addCriteria(notDeletedCriteria());
        query.addCriteria(Criteria.where("approvalStatus.status").is("PENDING"));

        Criteria scopeCriteria = buildScopedStudentCriteria(actor, branch);
        if (scopeCriteria != null) {
            query.addCriteria(scopeCriteria);
        }
        return query;
    }

    private AdminActor verifyAdminActor(HttpServletRequest request) {
        SuperAdminClaims superAdminClaims = (SuperAdminClaims) request.getAttribute("superAdminClaims");
        if (superAdminClaims != null) {
            if (
                !Boolean.TRUE.equals(superAdminClaims.getNoScopeRestriction())
                    || !"FULL".equals(superAdminClaims.getPlatformAccess())
            ) {
                throw new ForbiddenException("Super Admin access required");
            }
            return new AdminActor(superAdminClaims.getUserId(), true, new ArrayList<>());
        }

        AdminClaims adminClaims = (AdminClaims) request.getAttribute("adminClaims");
        if (adminClaims == null || !Boolean.TRUE.equals(adminClaims.getIsActive())) {
            throw new ForbiddenException("Admin access required");
        }

        CollegeAdmin admin = collegeAdminRepository.findById(adminClaims.getUserId())
            .orElseThrow(() -> new ForbiddenException("Admin access required"));

        if (!Boolean.TRUE.equals(admin.getIsActive()) || Boolean.TRUE.equals(admin.getIsDeleted())) {
            throw new ForbiddenException("Admin deactivated");
        }

        List<Scope> scopes = admin.getScopes() == null ? new ArrayList<>() : admin.getScopes();
        if (scopes.isEmpty()) {
            throw new ForbiddenException("No access scopes assigned");
        }

        return new AdminActor(admin.getId(), false, scopes);
    }

    private Student findStudentOrThrow(String studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("No student with this ID"));

        if (!"STUDENT".equals(student.getRole()) || Boolean.TRUE.equals(student.getIsDeleted())) {
            throw new ResourceNotFoundException("No student with this ID");
        }

        return student;
    }

    private Exam findExamOrThrow(String testId) {
        Exam exam = examRepository.findById(testId)
            .orElseThrow(() -> new ResourceNotFoundException("No test with this ID"));

        if (Boolean.TRUE.equals(exam.getIsDeleted())) {
            throw new ResourceNotFoundException("No test with this ID");
        }

        return exam;
    }

    private void ensurePending(Student student) {
        ApprovalStatus approvalStatus = student.getApprovalStatus();
        if (approvalStatus == null || !"PENDING".equals(approvalStatus.getStatus())) {
            throw new ConflictException("Student not in PENDING status");
        }
    }

    private void ensureStudentInScope(AdminActor actor, Student student) {
        if (actor.superAdmin()) {
            return;
        }

        boolean inScope = actor.scopes()
            .stream()
            .anyMatch(scope ->
                safeEquals(scope.getState(), student.getState())
                    && safeEquals(scope.getCollege(), student.getCollege())
                    && safeEquals(scope.getBranch(), student.getBranch())
            );

        if (!inScope) {
            throw new ForbiddenException("Student not in admin's scope");
        }
    }

    private void ensureExamInScope(AdminActor actor, Exam exam) {
        if (actor.superAdmin()) {
            return;
        }

        if (!StringUtils.hasText(exam.getState()) || !StringUtils.hasText(exam.getCollege()) || !StringUtils.hasText(exam.getBranch())) {
            throw new ForbiddenException("Test not in admin's scope");
        }

        boolean inScope = actor.scopes()
            .stream()
            .anyMatch(scope ->
                safeEquals(scope.getState(), exam.getState())
                    && safeEquals(scope.getCollege(), exam.getCollege())
                    && safeEquals(scope.getBranch(), exam.getBranch())
            );

        if (!inScope) {
            throw new ForbiddenException("Test not in admin's scope");
        }
    }

    private Criteria buildScopedStudentCriteria(AdminActor actor, String branch) {
        if (actor.superAdmin()) {
            if (StringUtils.hasText(branch)) {
                return Criteria.where("branch").is(branch);
            }
            return null;
        }

        List<Criteria> scopeCriteria = new ArrayList<>();
        for (Scope scope : actor.scopes()) {
            if (!StringUtils.hasText(scope.getState()) || !StringUtils.hasText(scope.getCollege()) || !StringUtils.hasText(scope.getBranch())) {
                continue;
            }

            if (StringUtils.hasText(branch) && !branch.equals(scope.getBranch())) {
                continue;
            }

            scopeCriteria.add(
                new Criteria().andOperator(
                    Criteria.where("state").is(scope.getState()),
                    Criteria.where("college").is(scope.getCollege()),
                    Criteria.where("branch").is(scope.getBranch())
                )
            );
        }

        if (scopeCriteria.isEmpty()) {
            return Criteria.where("_id").is("__NO_SCOPE_MATCH__");
        }

        return new Criteria().orOperator(scopeCriteria.toArray(new Criteria[0]));
    }

    private String resolveSortField(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "aptitudeHistory.score";
        }

        return switch (sortBy.toLowerCase()) {
            case "name" -> "name";
            case "timetaken" -> "aptitudeHistory.timeTaken";
            case "score" -> "aptitudeHistory.score";
            default -> "aptitudeHistory.score";
        };
    }

    private Criteria notDeletedCriteria() {
        return new Criteria().orOperator(
            Criteria.where("isDeleted").exists(false),
            Criteria.where("isDeleted").is(false)
        );
    }

    private ApprovalStudentResponse toApprovalStudentResponse(Student student) {
        return ApprovalStudentResponse.builder()
            .id(student.getId())
            .name(student.getName())
            .email(student.getEmail())
            .state(student.getState())
            .college(student.getCollege())
            .branch(student.getBranch())
            .yearOfPassing(student.getYearOfPassing())
            .approvalStatus(toApprovalStatusResponse(student.getApprovalStatus()))
            .createdAt(student.getCreatedAt())
            .build();
    }

    private StudentApprovalActionResponse toStudentApprovalActionResponse(Student student) {
        return StudentApprovalActionResponse.builder()
            .id(student.getId())
            .name(student.getName())
            .approvalStatus(toApprovalStatusResponse(student.getApprovalStatus()))
            .build();
    }

    private AdminStudentDetailResponse toAdminStudentDetailResponse(Student student) {
        return AdminStudentDetailResponse.builder()
            .id(student.getId())
            .name(student.getName())
            .email(student.getEmail())
            .role(student.getRole())
            .state(student.getState())
            .college(student.getCollege())
            .branch(student.getBranch())
            .yearOfPassing(student.getYearOfPassing())
            .approvalStatus(toApprovalStatusResponse(student.getApprovalStatus()))
            .aptitudeHistory(student.getAptitudeHistory() == null ? new ArrayList<>() : student.getAptitudeHistory())
            .createdAt(student.getCreatedAt())
            .build();
    }

    private StudentCompareItemResponse toStudentCompareItemResponse(Student student) {
        List<AptitudeHistory> aptitudeHistory = student.getAptitudeHistory() == null
            ? Collections.emptyList()
            : student.getAptitudeHistory();

        return StudentCompareItemResponse.builder()
            .id(student.getId())
            .name(student.getName())
            .email(student.getEmail())
            .state(student.getState())
            .college(student.getCollege())
            .branch(student.getBranch())
            .yearOfPassing(student.getYearOfPassing())
            .approvalStatus(toApprovalStatusResponse(student.getApprovalStatus()))
            .stats(computeStats(aptitudeHistory))
            .aptitudeHistory(aptitudeHistory)
            .build();
    }

    private StudentCompareStatsResponse computeStats(List<AptitudeHistory> aptitudeHistory) {
        if (aptitudeHistory == null || aptitudeHistory.isEmpty()) {
            return StudentCompareStatsResponse.builder()
                .totalAttempts(0)
                .averageScore(0.0)
                .averageTimeTaken(0.0)
                .practiceAttempts(0)
                .scheduledAttempts(0)
                .build();
        }

        int totalAttempts = aptitudeHistory.size();
        int practiceAttempts = 0;
        int scheduledAttempts = 0;

        double totalScore = 0.0;
        int scoreCount = 0;

        double totalTimeTaken = 0.0;
        int timeCount = 0;

        for (AptitudeHistory history : aptitudeHistory) {
            if (history.getScore() != null) {
                totalScore += history.getScore();
                scoreCount++;
            }

            if (history.getTimeTaken() != null) {
                totalTimeTaken += history.getTimeTaken();
                timeCount++;
            }

            if ("PRACTICE".equalsIgnoreCase(history.getType())) {
                practiceAttempts++;
            }

            if ("SCHEDULED".equalsIgnoreCase(history.getType())) {
                scheduledAttempts++;
            }
        }

        return StudentCompareStatsResponse.builder()
            .totalAttempts(totalAttempts)
            .averageScore(scoreCount == 0 ? 0.0 : totalScore / scoreCount)
            .averageTimeTaken(timeCount == 0 ? 0.0 : totalTimeTaken / timeCount)
            .practiceAttempts(practiceAttempts)
            .scheduledAttempts(scheduledAttempts)
            .build();
    }

    private AdminHistoryItemResponse toAdminHistoryItemResponse(Student student) {
        ApprovalStatus approvalStatus = student.getApprovalStatus();
        return AdminHistoryItemResponse.builder()
            .studentId(student.getId())
            .studentName(student.getName())
            .email(student.getEmail())
            .college(student.getCollege())
            .branch(student.getBranch())
            .action(approvalStatus == null ? null : approvalStatus.getStatus())
            .actionAt(approvalStatus == null ? null : approvalStatus.getApprovedAt())
            .rejectionReason(approvalStatus == null ? null : approvalStatus.getRejectionReason())
            .build();
    }

    private ApprovalStatusResponse toApprovalStatusResponse(ApprovalStatus status) {
        if (status == null) {
            return null;
        }

        return ApprovalStatusResponse.builder()
            .status(status.getStatus())
            .approvedBy(status.getApprovedBy())
            .approvedAt(status.getApprovedAt())
            .rejectionReason(status.getRejectionReason())
            .build();
    }

    private List<ScopeDto> toScopeDtos(List<Scope> scopes) {
        if (scopes == null) {
            return new ArrayList<>();
        }

        List<ScopeDto> result = new ArrayList<>();
        for (Scope scope : scopes) {
            result.add(
                ScopeDto.builder()
                    .state(scope.getState())
                    .college(scope.getCollege())
                    .branch(scope.getBranch())
                    .build()
            );
        }
        return result;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }

    private long numberToLong(Object value) {
        if (!(value instanceof Number number)) {
            return 0L;
        }
        return number.longValue();
    }

    private Double numberToNullableDouble(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        return number.doubleValue();
    }

    private Integer numberToNullableInteger(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        return number.intValue();
    }

    private Instant toInstant(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String dateString) {
            try {
                return Instant.parse(dateString);
            } catch (RuntimeException ex) {
                return null;
            }
        }
        return null;
    }

    private record AdminActor(String userId, boolean superAdmin, List<Scope> scopes) {}
}
