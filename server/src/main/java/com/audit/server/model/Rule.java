package com.audit.server.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "Rule")
public class Rule {
    @Getter @Setter
    private ObjectId id;

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
    private Guideline[] guidelines;

    public Rule(){

    }

    public Rule(String refId, String title, String description, String url) {
        this.refId = refId;
        this.title = title;
        this.description = description;
        this.url = url;
    }
}
