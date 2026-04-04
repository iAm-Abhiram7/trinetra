package com.trinetra.project.student.repository;

import com.trinetra.project.student.model.Student;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends MongoRepository<Student, String> {

    Optional<Student> findByEmailIgnoreCase(String email);
}
