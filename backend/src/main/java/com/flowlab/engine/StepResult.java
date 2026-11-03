package com.flowlab.engine;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StepResult {

    public enum ResultStatus {
        SUCCESS,
        RETRY,
        FAIL
    }

    private final ResultStatus status;
    private final Map<String, Object> output;
    private final String errorMessage;
    private final Duration retryDelay;

    private StepResult(ResultStatus status, Map<String, Object> output, String errorMessage, Duration retryDelay) {
        this.status = status;
        this.output = output != null ? new HashMap<>(output) : Collections.emptyMap();
        this.errorMessage = errorMessage;
        this.retryDelay = retryDelay;
    }

    public static StepResult success() {
        return new StepResult(ResultStatus.SUCCESS, Collections.emptyMap(), null, null);
    }

    public static StepResult success(Map<String, Object> output) {
        return new StepResult(ResultStatus.SUCCESS, output, null, null);
    }

    public static StepResult retry(String errorMessage) {
        return new StepResult(ResultStatus.RETRY, Collections.emptyMap(), errorMessage, null);
    }

    public static StepResult retry(String errorMessage, Duration retryDelay) {
        return new StepResult(ResultStatus.RETRY, Collections.emptyMap(), errorMessage, retryDelay);
    }

    public static StepResult fail(String errorMessage) {
        return new StepResult(ResultStatus.FAIL, Collections.emptyMap(), errorMessage, null);
    }

    public ResultStatus getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == ResultStatus.SUCCESS;
    }

    public boolean isRetry() {
        return status == ResultStatus.RETRY;
    }

    public boolean isFail() {
        return status == ResultStatus.FAIL;
    }

    public Map<String, Object> getOutput() {
        return Collections.unmodifiableMap(output);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }
}
