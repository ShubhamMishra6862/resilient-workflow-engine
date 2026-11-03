package com.flowlab.api.dto;

import com.flowlab.domain.WorkflowEventType;
import java.time.Instant;

public record WorkflowEventResponse(
        Long id,
        String executionId,
        WorkflowEventType eventType,
        String stepName,
        String detailJson,
        Instant createdAt
) {}
