package com.trinetra.project.student.model;

import com.trinetra.project.student.model.embedded.ApprovalStatus;
import com.trinetra.project.student.model.embedded.AptitudeHistory;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class Student {

    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("email")
    private String email;

    @Field("passwordHash")
    // SECURITY: Never serialize to client.
    private String passwordHash;

    @Field("role")
    private String role;

    @Field("state")
    private String state;

    @Field("college")
    private String college;

    @Field("branch")
    private String branch;

    @Field("yearOfPassing")
    private Integer yearOfPassing;

    @Field("approvalStatus")
    private ApprovalStatus approvalStatus;

    @Field("aptitudeHistory")
    private List<AptitudeHistory> aptitudeHistory;

    @Field("otpHash")
    // SECURITY: Never serialize to client.
    private String otpHash;

    @Field("otpExpiry")
    // SECURITY: Never serialize to client.
    private Instant otpExpiry;

    @Field("lastLoginAt")
    private Instant lastLoginAt;

    @Field("isDeleted")
    private Boolean isDeleted;

    @Field("deletedAt")
    private Instant deletedAt;

    @Field("deletedBy")
    private String deletedBy;

    @Field("createdAt")
    private Instant createdAt;
}
