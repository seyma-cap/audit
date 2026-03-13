package com.audit.server.rest;

import com.audit.server.projection.SuccessCriteriaProjection;
import com.audit.server.repo.SuccessCriteriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/criteria")
public class SuccessCriteriaController {

    @Autowired
    SuccessCriteriaRepository repo;

    @GetMapping(path = "/all", produces = "application/json")
    public List<SuccessCriteriaProjection> getAllGuidelines() {
        return repo.findAllSuccessCriteriaOnly();
    }
}
