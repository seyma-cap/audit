package com.audit.server.repo;

import com.audit.server.model.Audit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AuditRepository extends MongoRepository<Audit, String> {
    @Query(value = "{ 'url': ?0 }")
    Audit findByURL(String refId);
}
