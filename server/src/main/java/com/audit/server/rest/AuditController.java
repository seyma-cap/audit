package com.audit.server.rest;

import com.audit.server.dto.AnswerRequest;
import com.audit.server.dto.AnswerTemplate;
import com.audit.server.dto.UrlRequest;
import com.audit.server.model.*;
import com.audit.server.projection.SuccessCriteriaProjection;
import com.audit.server.repo.AuditRepository;
import com.audit.server.repo.SuccessCriteriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping( "/audit")
public class AuditController {

    @Autowired
    private AuditRepository repo;

    @Autowired
    private SuccessCriteriaRepository criteriaRepo;

    @GetMapping(path = "/all", produces = "application/json")
    public List<Audit> getAllAudits(){
        return repo.findAll();
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public Optional<Audit> getById(@PathVariable String id){
        return repo.findById(id);
    }

    @GetMapping(path = "getAnswer", produces = "application/json")
    public ResponseEntity<AuditAnswer> getAnswersById(@RequestParam String id, @RequestParam String refId){
        Audit audit = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return audit.getAuditAnswers()
                .stream()
                .filter(a -> a.getSuccessCriteria().getRefId().equals(refId))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    }

    @GetMapping(path = "/scores", produces = "application/json")
    public Score[] getScoreTypes(){
        return Score.values();
    }

    @PostMapping("/saveUrl")
    public ResponseEntity<Audit> saveUrl(@RequestBody UrlRequest body){
        Audit a = new Audit();
        a.setUrl(body.getUrl());

        Audit save = repo.save(a);
        return ResponseEntity.status(HttpStatus.OK).body(save);
    }

    @PutMapping("/saveAnswers")
    public ResponseEntity<Audit> saveAnswers(@RequestBody AnswerRequest body) {
        Audit audit = repo.findById(body.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        SuccessCriteria criteria = criteriaRepo
                .findBySuccessCriteriaRefId(body.refId())
                .getSuccessCriteria()
                .getFirst();

        Optional<AuditAnswer> existingAnswer = audit.getAuditAnswers()
                .stream()
                .filter(a -> a.getSuccessCriteria().getRefId().equals(criteria.getRefId()))
                .findFirst();

        if (existingAnswer.isPresent()) {
            AuditAnswer answer = existingAnswer.get();
            answer.setScore(body.score());
            answer.setAnswers(body.answers());

        } else {
            AuditAnswer answer = new AuditAnswer();
            answer.setSuccessCriteria(criteria);
            answer.setScore(body.score());
            answer.setAnswers(body.answers());

            audit.addAnswers(answer);
        }

        return ResponseEntity.ok(repo.save(audit));
    }
}
