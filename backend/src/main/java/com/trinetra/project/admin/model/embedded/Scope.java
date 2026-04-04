package com.trinetra.project.admin.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scope {

    @Field("state")
    private String state;

    @Field("college")
    private String college;

    @Field("branch")
    private String branch;
}
