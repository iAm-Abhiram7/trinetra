package com.trinetra.project.student.service;

import com.mongodb.client.result.UpdateResult;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.student.model.Student;
import com.trinetra.project.student.model.embedded.AptitudeHistory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class AptitudeHistoryServiceImpl implements AptitudeHistoryService {

    private final MongoTemplate usersMongoTemplate;

    public AptitudeHistoryServiceImpl(@Qualifier("usersMongoTemplate") MongoTemplate usersMongoTemplate) {
        this.usersMongoTemplate = usersMongoTemplate;
    }

    @Override
    public void appendToHistory(String studentId, AptitudeHistory entry) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(studentId));
        query.addCriteria(Criteria.where("role").is("STUDENT"));
        query.addCriteria(
            new Criteria().orOperator(
                Criteria.where("isDeleted").exists(false),
                Criteria.where("isDeleted").is(false)
            )
        );

        Update update = new Update().push("aptitudeHistory").slice(-50).each(entry);

        UpdateResult result = usersMongoTemplate.updateFirst(query, update, Student.class);
        if (result.getMatchedCount() == 0) {
            throw new ResourceNotFoundException("No student with this ID");
        }
    }
}
