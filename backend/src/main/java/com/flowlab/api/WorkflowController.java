package com.flowlab.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowlab.api.dto.WorkflowDefinitionResponse;
import com.flowlab.domain.WorkflowDefinition;
import com.flowlab.repository.WorkflowDefinitionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final WorkflowDefinitionRepository definitionRepository;

    public WorkflowController(WorkflowDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDefinitionResponse>> listWorkflows() {
        List<WorkflowDefinition> definitions = definitionRepository.findAllByOrderByCreatedAtDesc();
        List<WorkflowDefinitionResponse> responses = definitions.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{name}")
    public ResponseEntity<WorkflowDefinitionResponse> getWorkflow(@PathVariable String name) {
        return definitionRepository.findByNameAndActiveTrue(name)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private WorkflowDefinitionResponse toResponse(WorkflowDefinition d) {
        List<String> steps = Collections.emptyList();
        try {
            steps = OBJECT_MAPPER.readValue(d.getStepsJson(), new TypeReference<List<String>>() {});
        } catch (Exception ignored) {}

        return new WorkflowDefinitionResponse(
                d.getId(),
                d.getName(),
                d.getVersion(),
                d.getDescription(),
                steps,
                d.getActive(),
                d.getCreatedAt()
        );
    }
}
