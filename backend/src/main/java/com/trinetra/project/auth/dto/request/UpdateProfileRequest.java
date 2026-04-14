package com.trinetra.project.auth.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String name;
    private String state;
    private String college;
    private String branch;

    @Min(value = 2000, message = "Year of passing is invalid")
    private Integer yearOfPassing;
}
