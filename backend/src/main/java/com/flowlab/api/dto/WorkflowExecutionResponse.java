package com.flowlab.api.dto;

import com.flowlab.domain.WorkflowStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowExecutionResponse(
        String id,
        String workflowName,
        WorkflowStatus status,
        Integer currentStepIndex,
        Map<String, Object> context,
        Integer version,
        Instant createdAt,
        Instant updatedAt,
        List<StepExecutionResponse> steps
) {}
