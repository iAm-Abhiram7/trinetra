package com.trinetra.project.student.model.embedded;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStatus {

    @Field("status")
    private String status;

    @Field("approvedBy")
    private String approvedBy;

    @Field("approvedAt")
    private Instant approvedAt;

    @Field("rejectionReason")
    private String rejectionReason;
}
