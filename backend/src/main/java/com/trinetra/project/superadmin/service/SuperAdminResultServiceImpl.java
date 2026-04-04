package com.trinetra.project.superadmin.service;

import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.security.claims.SuperAdminClaims;
import com.trinetra.project.exam.model.Exam;
import com.trinetra.project.superadmin.dto.response.AggregatedResultResponse;
import com.trinetra.project.superadmin.dto.response.TestResultSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SuperAdminResultServiceImpl implements SuperAdminResultService {

    private final MongoTemplate examsMongoTemplate;
    private final MongoTemplate usersMongoTemplate;

    public SuperAdminResultServiceImpl(
        @Qualifier("examsMongoTemplate") MongoTemplate examsMongoTemplate,
        @Qualifier("usersMongoTemplate") MongoTemplate usersMongoTemplate
    ) {
        this.examsMongoTemplate = examsMongoTemplate;
        this.usersMongoTemplate = usersMongoTemplate;
    }

    @Override
    public TestResultSummaryResponse getAggregatedResults(HttpServletRequest request, int page, int limit, String testId) {
        SuperAdminClaims claims = verifySuperAdmin(request);

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 20 : limit;

        Query examQuery = new Query();
        examQuery.addCriteria(Criteria.where("createdBy").is(claims.getUserId()));
        examQuery.addCriteria(notDeletedCriteria());

        if (StringUtils.hasText(testId)) {
            examQuery.addCriteria(Criteria.where("_id").is(testId));
        }

        Query countQuery = Query.of(examQuery).limit(-1).skip(-1);
        examQuery
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .skip((long) (safePage - 1) * safeLimit)
            .limit(safeLimit);

        List<Exam> exams = examsMongoTemplate.find(examQuery, Exam.class);
        long totalTests = examsMongoTemplate.count(countQuery, Exam.class);
        int totalPages = totalTests == 0 ? 0 : (int) Math.ceil((double) totalTests / safeLimit);

        List<AggregatedResultResponse> results = new ArrayList<>();
        for (Exam exam : exams) {
            results.add(computeAggregatedResult(exam));
        }

        return TestResultSummaryResponse.builder()
            .testResults(results)
            .currentPage(safePage)
            .totalPages(totalPages)
            .totalTests(totalTests)
            .limit(safeLimit)
            .build();
    }

    private AggregatedResultResponse computeAggregatedResult(Exam exam) {
        Criteria baseStudentCriteria = new Criteria().andOperator(
            Criteria.where("role").is("STUDENT"),
            notDeletedCriteria()
        );

        Criteria attemptCriteria;
        if ("PRACTICE".equals(exam.getType())) {
            attemptCriteria = new Criteria().orOperator(
                Criteria.where("aptitudeHistory.examId").is(exam.getId()),
                new Criteria().andOperator(
                    Criteria.where("aptitudeHistory.examId").is(null),
                    Criteria.where("aptitudeHistory.topic").is(exam.getTitle())
                )
            );
        } else {
            attemptCriteria = Criteria.where("aptitudeHistory.examId").is(exam.getId());
        }

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(baseStudentCriteria),
            Aggregation.unwind("aptitudeHistory"),
            Aggregation.match(attemptCriteria),
            Aggregation.group()
                .count().as("totalStudentsAttempted")
                .avg("aptitudeHistory.score").as("averageScore")
                .max("aptitudeHistory.score").as("highestScore")
                .min("aptitudeHistory.score").as("lowestScore")
                .avg("aptitudeHistory.timeTaken").as("averageTimeTaken")
        );

        AggregationResults<Document> aggregationResults = usersMongoTemplate.aggregate(
            aggregation,
            "users",
            Document.class
        );

        Document stats = aggregationResults.getUniqueMappedResult();

        long totalStudentsAttempted = numberToLong(stats == null ? null : stats.get("totalStudentsAttempted"));
        double averageScore = numberToDouble(stats == null ? null : stats.get("averageScore"));
        double highestScore = numberToDouble(stats == null ? null : stats.get("highestScore"));
        double lowestScore = numberToDouble(stats == null ? null : stats.get("lowestScore"));
        double averageTimeTaken = numberToDouble(stats == null ? null : stats.get("averageTimeTaken"));

        double averagePercentage = 0.0;
        if (exam.getTotalMarks() != null && exam.getTotalMarks() > 0) {
            averagePercentage = (averageScore / exam.getTotalMarks()) * 100.0;
        }

        return AggregatedResultResponse.builder()
            .testId(exam.getId())
            .title(exam.getTitle())
            .type(exam.getType())
            .college(exam.getCollege())
            .branch(exam.getBranch())
            .totalStudentsAttempted(totalStudentsAttempted)
            .averageScore(averageScore)
            .highestScore(highestScore)
            .lowestScore(lowestScore)
            .averageTimeTaken(averageTimeTaken)
            .averagePercentage(averagePercentage)
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

    private long numberToLong(Object numberValue) {
        if (!(numberValue instanceof Number number)) {
            return 0L;
        }
        return number.longValue();
    }

    private double numberToDouble(Object numberValue) {
        if (!(numberValue instanceof Number number)) {
            return 0.0;
        }
        return number.doubleValue();
    }
}
