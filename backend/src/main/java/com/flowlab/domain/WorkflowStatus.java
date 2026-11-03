package com.flowlab.domain;

public enum WorkflowStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == COMPENSATED || this == CANCELLED;
    }
}
