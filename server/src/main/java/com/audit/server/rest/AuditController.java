package com.audit.server.rest;

import com.audit.server.dto.UrlRequest;
import com.audit.server.model.Audit;
import com.audit.server.repo.AuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping( "/audit")
public class AuditController {

    @Autowired
    private AuditRepository repo;

    @PostMapping("/saveUrl")
    public ResponseEntity<Audit> saveUrl(@RequestBody UrlRequest body){
        Audit a = new Audit();
        a.setUrl(body.getUrl());

        Audit save = repo.save(a);
        return ResponseEntity.status(HttpStatus.OK).body(save);
    }
}
