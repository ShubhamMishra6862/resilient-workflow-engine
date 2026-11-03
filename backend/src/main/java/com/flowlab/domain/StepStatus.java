package com.flowlab.domain;

public enum StepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    RETRY_SCHEDULED,
    FAILED,
    COMPENSATED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == COMPENSATED;
    }
}
