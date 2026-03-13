package com.audit.server.repo;


import com.audit.server.model.Guideline;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GuidelineRepository extends MongoRepository<Guideline, String> {
}

