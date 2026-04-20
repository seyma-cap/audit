package com.audit.server.dto;

import com.audit.server.model.Score;

public record AnswerRequest(
        String id,
        String refId,
        Score score,
        AnswerTemplate[] answers
) {
}
