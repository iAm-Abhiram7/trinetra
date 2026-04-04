package com.trinetra.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = "com.trinetra.project.exam.repository",
    mongoTemplateRef = "examsMongoTemplate"
)
public class ExamsMongoRepositoryConfig {
}
