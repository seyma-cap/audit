package com.audit.server.rest;

import com.audit.server.service.JSoupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    JSoupService jSoupService;

    @GetMapping(path = "", produces = "application/json")
    public ResponseEntity<String> testApi(){
        jSoupService.getData();
        String response = "Hello World";
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
