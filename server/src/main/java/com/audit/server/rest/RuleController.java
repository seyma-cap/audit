package com.audit.server.rest;

import com.audit.server.model.Rule;
import com.audit.server.repo.RuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rules")
public class RuleController {

    @Autowired
    RuleRepository repo;

    @GetMapping(path = "/all", produces = "application/json")
    public List<Rule> getAllRules(){
        return repo.findAll();
    }
}
