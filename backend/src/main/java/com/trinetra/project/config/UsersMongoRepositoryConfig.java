package com.trinetra.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = {
        "com.trinetra.project.student.repository",
        "com.trinetra.project.admin.repository"
    },
    mongoTemplateRef = "usersMongoTemplate"
)
public class UsersMongoRepositoryConfig {
}
