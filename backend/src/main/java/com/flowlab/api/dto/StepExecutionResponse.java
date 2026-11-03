package com.flowlab.api.dto;

import com.flowlab.domain.StepStatus;
import java.time.Instant;

public record StepExecutionResponse(
        String id,
        String stepName,
        Integer stepIndex,
        StepStatus status,
        Integer attemptCount,
        Integer maxAttempts,
        Instant nextRetryAt,
        Instant leaseExpiresAt,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        String error
) {}
