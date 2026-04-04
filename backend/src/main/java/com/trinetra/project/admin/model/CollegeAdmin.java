package com.trinetra.project.admin.model;

import com.trinetra.project.admin.model.embedded.Scope;
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
@Document(collection = "collegeAdmins")
public class CollegeAdmin {

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

    @Field("isActive")
    private Boolean isActive;

    @Field("scopes")
    private List<Scope> scopes;

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
