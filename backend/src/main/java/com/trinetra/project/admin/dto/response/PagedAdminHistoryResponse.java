package com.trinetra.project.admin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedAdminHistoryResponse {

    private List<AdminHistoryItemResponse> history;
    private int currentPage;
    private int totalPages;
    private long totalRecords;
    private int limit;
}
