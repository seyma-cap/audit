package com.audit.server.dto;

import org.bson.types.ObjectId;

public record GuidelineResponse(
        ObjectId id,
        String refId,
        String title
) {
}
