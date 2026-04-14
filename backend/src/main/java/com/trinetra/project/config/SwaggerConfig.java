package com.trinetra.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI platformOpenApi() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("Student Assessment Platform API")
                    .version("1.0")
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .schemaRequirement(
                "bearerAuth",
                new SecurityScheme()
                    .name("bearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )
            .addTagsItem(new Tag().name("Super Admin - Tests"))
            .addTagsItem(new Tag().name("Super Admin - Users"))
            .addTagsItem(new Tag().name("Super Admin - Admins"))
            .addTagsItem(new Tag().name("Super Admin - Results"))
            .addTagsItem(new Tag().name("Admin Approvals"))
            .addTagsItem(new Tag().name("Admin Students"))
            .addTagsItem(new Tag().name("Admin History"))
            .addTagsItem(new Tag().name("Auth - Student Authentication"))
            .addTagsItem(new Tag().name("Content - Articles"))
            .addTagsItem(new Tag().name("Practice - Exam"))
            .addTagsItem(new Tag().name("Scheduled - Test"));
    }
}
