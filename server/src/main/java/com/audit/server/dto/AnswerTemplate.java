package com.audit.server.dto;

public record AnswerTemplate (
        String title,
        String description,
        String recommendation,
        String comment
) {
}
