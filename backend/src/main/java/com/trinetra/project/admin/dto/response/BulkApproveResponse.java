package com.trinetra.project.admin.dto.response;

import com.trinetra.project.superadmin.dto.response.ScopeDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkApproveResponse {

    private long approvedCount;
    private List<ScopeDto> scope;
}
