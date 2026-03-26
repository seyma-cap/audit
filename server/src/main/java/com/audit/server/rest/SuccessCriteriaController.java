package com.audit.server.rest;

import com.audit.server.projection.SuccessCriteriaProjection;
import com.audit.server.repo.SuccessCriteriaRepository;
import com.audit.server.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/criteria")
public class SuccessCriteriaController {

    @Autowired
    SuccessCriteriaRepository repo;

    @Autowired
    private AiService aiService;

    @GetMapping(path = "/all", produces = "application/json")
    public List<SuccessCriteriaProjection> getAllGuidelines() {
        return repo.findAllSuccessCriteriaOnly();
    }

    @GetMapping(path = "/ai_put", produces = "application/json")
    public Map<String, String> getAiRecommendation(@RequestParam String criteriaId, @RequestParam String auditId) {
        return aiService.generateResponse(criteriaId, auditId);
    }
}
