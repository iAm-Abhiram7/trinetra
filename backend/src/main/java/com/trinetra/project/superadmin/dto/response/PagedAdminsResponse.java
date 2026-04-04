package com.trinetra.project.superadmin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedAdminsResponse {

    private List<AdminSummaryResponse> admins;
    private int currentPage;
    private int totalPages;
    private long totalAdmins;
    private int limit;
}
