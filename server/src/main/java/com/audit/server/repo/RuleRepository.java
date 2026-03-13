package com.audit.server.repo;

import com.audit.server.model.Rule;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RuleRepository extends MongoRepository<Rule,String> {
}
