package com.audit.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuditAnswer {
    @Id
    private String id;

    private SuccessCriteria successCriteria;

    private Score score;

    private String title;

    private String description;

    private String recommendation;

    private String comment;
}
