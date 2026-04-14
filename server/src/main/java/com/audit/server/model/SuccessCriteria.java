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

    @Getter @Setter
    private String fetchType;
    // TODO add a object to store the answers (thinking List<>?)

    public SuccessCriteria() {

    }


    public SuccessCriteria(String refId, String title, String description, String url, String level, String fetchType) {
        this.refId = refId;
        this.title = title;
        this.description = description;
        this.url = url;
        this.level = level;
        this.fetchType = fetchType;
    }
}
