package com.audit.server.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document
public class Audit {
    @Id
    private String id;

    private String url;

    private AuditAnswer[] auditAnswers;
}
