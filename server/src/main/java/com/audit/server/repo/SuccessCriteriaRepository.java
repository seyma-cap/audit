package com.audit.server.repo;

import com.audit.server.model.Guideline;
import com.audit.server.projection.SuccessCriteriaProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface SuccessCriteriaRepository extends MongoRepository<Guideline,String> {
    @Query(value = "{}", fields = "{ 'success_criteria': 1, '_id': 0 }")
    List<SuccessCriteriaProjection> findAllSuccessCriteriaOnly();
}
