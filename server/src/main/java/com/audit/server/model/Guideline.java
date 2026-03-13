package com.audit.server.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

public class Guideline {
    @Getter @Setter
    @Field("ref_id")
    private String refId;

    @Getter @Setter
    private String title;

    @Getter @Setter
    private String description;

    @Getter @Setter
    private String url;

    @Getter @Setter
    @Field("success_criteria")
    private SuccessCriteria[] successCriteria;

    public Guideline(){

    }

    public Guideline(String refId, String title, String description, String url, SuccessCriteria[] successCriteria) {
        this.refId = refId;
        this.title = title;
        this.description = description;
        this.url = url;
        this.successCriteria = successCriteria;
    }
}
