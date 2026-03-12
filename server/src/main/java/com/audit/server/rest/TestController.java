package com.audit.server.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping(path = "", produces = "application/json")
    public ResponseEntity<String> testApi(){
        String response = "Hello World";
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
