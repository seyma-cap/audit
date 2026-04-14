package com.audit.server.rest;

import com.audit.server.dto.UrlRequest;
import com.audit.server.model.Audit;
import com.audit.server.repo.AuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping( "/audit")
public class AuditController {

    @Autowired
    private AuditRepository repo;

    @GetMapping(path = "/all", produces = "application/json")
    public List<Audit> getAllAudits(){
        return repo.findAll();
    }

    @PostMapping("/saveUrl")
    public ResponseEntity<Audit> saveUrl(@RequestBody UrlRequest body){
        Audit a = new Audit();
        a.setUrl(body.getUrl());

        Audit save = repo.save(a);
        return ResponseEntity.status(HttpStatus.OK).body(save);
    }
}
