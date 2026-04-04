package com.trinetra.project.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient(@Value("${app.mongodb.uri}") String uri) {
        return MongoClients.create(uri);
    }

    @Primary
    @Bean(name = "usersMongoTemplate")
    public MongoTemplate usersMongoTemplate(
        MongoClient mongoClient,
        @Value("${app.mongodb.users-database}") String usersDatabase
    ) {
        return new MongoTemplate(mongoClient, usersDatabase);
    }

    @Bean(name = "examsMongoTemplate")
    public MongoTemplate examsMongoTemplate(
        MongoClient mongoClient,
        @Value("${app.mongodb.exams-database}") String examsDatabase
    ) {
        return new MongoTemplate(mongoClient, examsDatabase);
    }
}
