package com.flowlab.api.dto;

import java.time.Instant;
import java.util.List;

public record WorkflowDefinitionResponse(
        Long id,
        String name,
        Integer version,
        String description,
        List<String> steps,
        Boolean active,
        Instant createdAt
) {}
