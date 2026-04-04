package com.trinetra.project.superadmin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCredResponse {

    private String adminId;
    private String name;
    private String email;
    private String college;
    private String branch;
    private Boolean isActive;
}
