package com.trinetra.project.superadmin.service;

import com.trinetra.project.superadmin.dto.request.CreateExamRequest;
import com.trinetra.project.superadmin.dto.response.DeleteTestResponse;
import com.trinetra.project.superadmin.dto.response.ExamDetailResponse;
import com.trinetra.project.superadmin.dto.response.PagedTestsResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface SuperAdminTestService {

    PagedTestsResponse getAllTests(
        HttpServletRequest request,
        int page,
        int limit,
        String type,
        Boolean isPublished,
        String search
    );

    ExamDetailResponse createTest(HttpServletRequest request, CreateExamRequest requestBody);

    DeleteTestResponse deleteTest(HttpServletRequest request, String testId);
}
