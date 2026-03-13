package com.audit.server.rest;

import com.audit.server.model.Guideline;
import com.audit.server.repo.GuidelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/guidelines")
public class GuidelineController {

    @Autowired
    private GuidelineRepository repo;

    @GetMapping(path = "/all", produces = "application/json")
    public List<Guideline> getAllGuidelines() {
        return repo.findAll();
    }
}

