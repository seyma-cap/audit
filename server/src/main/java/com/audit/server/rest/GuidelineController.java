package com.audit.server.rest;

import com.audit.server.model.Guideline;
import com.audit.server.repo.GuidelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/guidelines")
public class GuidelineController {

    @Autowired
    private GuidelineRepository repo;

    @GetMapping(path = "/all", produces = "application/json")
    public List<Guideline> getAllGuidelines() {
        return repo.findAll();
    }

    @GetMapping(path = "/ids", produces = "application/json")
    public List<String> getAllIds() {
        List<Guideline> guidelines = repo.findAll();
        List<String> ids = new ArrayList<>();

        for (Guideline g : guidelines){
            ids.add(String.valueOf(g.getId()));
        }

        return ids;
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public Optional<Guideline> getById(@PathVariable String id){
        return repo.findById(id);
    }
}

