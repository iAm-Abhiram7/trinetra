package com.trinetra.project.content.repository;

import com.trinetra.project.content.model.Article;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ArticleRepository {

    private final MongoTemplate usersMongoTemplate;

    public ArticleRepository(@Qualifier("usersMongoTemplate") MongoTemplate usersMongoTemplate) {
        this.usersMongoTemplate = usersMongoTemplate;
    }

    public Optional<Article> findActiveBySlug(String slug) {
        Query query = new Query();
        query.addCriteria(Criteria.where("slug").is(slug));
        query.addCriteria(
            new Criteria().orOperator(
                Criteria.where("isDeleted").exists(false),
                Criteria.where("isDeleted").is(false)
            )
        );
        return Optional.ofNullable(usersMongoTemplate.findOne(query, Article.class));
    }
}
