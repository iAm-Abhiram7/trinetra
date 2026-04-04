package com.trinetra.project.admin.repository;

import com.trinetra.project.admin.model.CollegeAdmin;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollegeAdminRepository extends MongoRepository<CollegeAdmin, String> {

    Optional<CollegeAdmin> findByEmailIgnoreCase(String email);
}
