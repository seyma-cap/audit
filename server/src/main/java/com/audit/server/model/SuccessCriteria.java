package com.audit.server.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

public class SuccessCriteria {
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
    private String level;
    // TODO add a object to store the answers (thinking List<>?)

    public SuccessCriteria() {

    }


    public SuccessCriteria(String refId, String title, String description, String url, String level) {
        this.refId = refId;
        this.title = title;
        this.description = description;
        this.url = url;
        this.level = level;
    }
}
