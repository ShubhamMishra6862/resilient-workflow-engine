package com.flowlab.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowlab.api.dto.StartWorkflowRequest;
import com.flowlab.api.dto.StepExecutionResponse;
import com.flowlab.api.dto.WorkflowEventResponse;
import com.flowlab.api.dto.WorkflowExecutionResponse;
import com.flowlab.domain.StepExecution;
import com.flowlab.domain.WorkflowEvent;
import com.flowlab.domain.WorkflowExecution;
import com.flowlab.domain.WorkflowStatus;
import com.flowlab.engine.WorkflowEngine;
import com.flowlab.repository.StepExecutionRepository;
import com.flowlab.repository.WorkflowEventRepository;
import com.flowlab.repository.WorkflowExecutionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ExecutionController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WorkflowEngine workflowEngine;
    private final WorkflowExecutionRepository executionRepository;
    private final StepExecutionRepository stepRepository;
    private final WorkflowEventRepository eventRepository;

    public ExecutionController(
            WorkflowEngine workflowEngine,
            WorkflowExecutionRepository executionRepository,
            StepExecutionRepository stepRepository,
            WorkflowEventRepository eventRepository
    ) {
        this.workflowEngine = workflowEngine;
        this.executionRepository = executionRepository;
        this.stepRepository = stepRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Start a new workflow execution.
     * Returns 202 Accepted with Location header.
     */
    @PostMapping("/workflows/{name}/executions")
    public ResponseEntity<WorkflowExecutionResponse> startWorkflow(
            @PathVariable String name,
            @RequestBody(required = false) StartWorkflowRequest request
    ) {
        Map<String, Object> input = (request != null && request.getInputData() != null)
                ? request.getInputData()
                : Collections.emptyMap();

        WorkflowExecution execution = workflowEngine.startWorkflow(name, input);
        WorkflowExecutionResponse response = toDetailedResponse(execution);

        URI location = URI.create("/api/v1/executions/" + execution.getId());
        return ResponseEntity.accepted().location(location).body(response);
    }

    /**
     * List executions with optional filters.
     */
    @GetMapping("/executions")
    public ResponseEntity<Page<WorkflowExecutionResponse>> listExecutions(
            @RequestParam(required = false) String workflowName,
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WorkflowExecution> results;

        if (workflowName != null && status != null) {
            results = executionRepository.findByWorkflowNameAndStatusOrderByCreatedAtDesc(workflowName, status, pageable);
        } else if (workflowName != null) {
            results = executionRepository.findByWorkflowNameOrderByCreatedAtDesc(workflowName, pageable);
        } else if (status != null) {
            results = executionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            results = executionRepository.findAll(pageable);
        }

        Page<WorkflowExecutionResponse> mapped = results.map(this::toDetailedResponse);
        return ResponseEntity.ok(mapped);
    }

    /**
     * Get single execution by ID with all steps.
     */
    @GetMapping("/executions/{id}")
    public ResponseEntity<WorkflowExecutionResponse> getExecution(@PathVariable String id) {
        return executionRepository.findByIdWithSteps(id)
                .map(this::toDetailedResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get event audit logs for an execution.
     */
    @GetMapping("/executions/{id}/events")
    public ResponseEntity<List<WorkflowEventResponse>> getEvents(@PathVariable String id) {
        List<WorkflowEvent> events = eventRepository.findByExecutionIdOrderByCreatedAtAsc(id);
        List<WorkflowEventResponse> responses = events.stream()
                .map(e -> new WorkflowEventResponse(
                        e.getId(),
                        e.getExecutionId(),
                        e.getEventType(),
                        e.getStepName(),
                        e.getDetailJson(),
                        e.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Manually retry a failed step.
     */
    @PostMapping("/executions/{id}/steps/{stepName}/retry")
    public ResponseEntity<Void> retryStep(@PathVariable String id, @PathVariable String stepName) {
        workflowEngine.manualRetry(id, stepName);
        return ResponseEntity.ok().build();
    }

    /**
     * Cancel an active execution.
     */
    @PostMapping("/executions/{id}/cancel")
    public ResponseEntity<Void> cancelExecution(@PathVariable String id) {
        workflowEngine.cancelWorkflow(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Simulates a worker crash (kill -9) on the running step for crash-proof demonstration.
     */
    @PostMapping("/executions/{id}/simulate-crash")
    public ResponseEntity<Map<String, Object>> simulateCrash(@PathVariable String id) {
        boolean crashed = workflowEngine.simulateCrash(id);
        return ResponseEntity.ok(Map.of(
                "success", crashed,
                "message", crashed ? "Worker crash simulated. Recovery scheduler will rescue within 3 seconds."
                                   : "No active step found in RUNNING state to crash."
        ));
    }

    private WorkflowExecutionResponse toDetailedResponse(WorkflowExecution execution) {
        List<StepExecution> steps = stepRepository.findByExecutionIdOrderByStepIndexAsc(execution.getId());
        List<StepExecutionResponse> stepResponses = steps.stream()
                .map(s -> new StepExecutionResponse(
                        s.getId(),
                        s.getStepName(),
                        s.getStepIndex(),
                        s.getStatus(),
                        s.getAttemptCount(),
                        s.getMaxAttempts(),
                        s.getNextRetryAt(),
                        s.getLeaseExpiresAt(),
                        s.getStartedAt(),
                        s.getCompletedAt(),
                        s.getDurationMs(),
                        s.getError()
                ))
                .toList();

        Map<String, Object> context = Collections.emptyMap();
        try {
            if (execution.getContextJson() != null && !execution.getContextJson().isEmpty()) {
                context = OBJECT_MAPPER.readValue(execution.getContextJson(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {}

        return new WorkflowExecutionResponse(
                execution.getId(),
                execution.getWorkflowName(),
                execution.getStatus(),
                execution.getCurrentStepIndex(),
                context,
                execution.getVersion(),
                execution.getCreatedAt(),
                execution.getUpdatedAt(),
                stepResponses
        );
    }
}
