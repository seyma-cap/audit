package com.audit.server.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document
public class Audit {
    @Id
    private String id;

    private String url;

    private List<AuditAnswer> auditAnswers = new ArrayList<>();

    public void addAnswers(AuditAnswer answer){
        auditAnswers.add(answer);
    }
}
