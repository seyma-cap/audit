package com.audit.server.dto;

import org.bson.types.ObjectId;

public record GuidelineResponse(
        String id,
        String refId,
        String title
) {
}
