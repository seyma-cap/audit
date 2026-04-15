package com.audit.server.model;

public enum Score {
    PASSED("passed"),
    FAILED("failed"),
    PASSED_REC("passed(recomm)"),
    NA("N/A");

    public final String label;

    Score(String label) {
        this.label = label;
    }
}
