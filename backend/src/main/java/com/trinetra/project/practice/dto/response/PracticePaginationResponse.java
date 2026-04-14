package com.trinetra.project.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticePaginationResponse {

    private Integer currentPage;
    private Integer totalPages;
    private Long totalAttempts;
    private Integer limit;
}
