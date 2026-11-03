package com.flowlab.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowlab.domain.*;
import com.flowlab.repository.StepExecutionRepository;
import com.flowlab.repository.WorkflowDefinitionRepository;
import com.flowlab.repository.WorkflowExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final StepExecutionRepository stepRepository;
    private final StepRegistry stepRegistry;
    private final RetryPolicy retryPolicy;
    private final WorkflowEventService eventService;

    @Value("${flowlab.lease.duration-seconds:30}")
    private long leaseDurationSeconds;

    public WorkflowEngine(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowExecutionRepository executionRepository,
            StepExecutionRepository stepRepository,
            StepRegistry stepRegistry,
            RetryPolicy retryPolicy,
            WorkflowEventService eventService
    ) {
        this.definitionRepository = definitionRepository;
        this.executionRepository = executionRepository;
        this.stepRepository = stepRepository;
        this.stepRegistry = stepRegistry;
        this.retryPolicy = retryPolicy;
        this.eventService = eventService;
    }

    /**
     * Starts a new workflow run from a registered definition.
     */
    @Transactional
    public WorkflowExecution startWorkflow(String workflowName, Map<String, Object> inputData) {
        WorkflowDefinition definition = definitionRepository.findByNameAndActiveTrue(workflowName)
                .orElseThrow(() -> new IllegalArgumentException("Active workflow definition not found: " + workflowName));

        List<String> stepNames = parseStepsJson(definition.getStepsJson());
        if (stepNames.isEmpty()) {
            throw new IllegalStateException("Workflow definition has no steps configured: " + workflowName);
        }

        String executionId = UUID.randomUUID().toString();
        String contextJson = "{}";
        try {
            if (inputData != null && !inputData.isEmpty()) {
                contextJson = OBJECT_MAPPER.writeValueAsString(inputData);
            }
        } catch (Exception e) {
            log.error("Failed to serialize input data for execution {}", executionId, e);
        }

        WorkflowExecution execution = new WorkflowExecution(executionId, workflowName, definition, contextJson);
        execution.setStatus(WorkflowStatus.RUNNING);
        execution.setCurrentStepIndex(0);

        for (int i = 0; i < stepNames.size(); i++) {
            String sName = stepNames.get(i);
            String stepId = UUID.randomUUID().toString();
            StepExecution step = new StepExecution(stepId, execution, sName, i, 3);
            execution.addStep(step);
        }

        WorkflowExecution savedExecution = executionRepository.save(execution);

        eventService.logEvent(executionId, WorkflowEventType.WORKFLOW_STARTED, null,
                Map.of("workflowName", workflowName, "totalSteps", stepNames.size()));

        // Trigger execution of the initial step asynchronously
        executeStepAsync(executionId, stepNames.get(0));

        return savedExecution;
    }

    /**
     * Executes a single step asynchronously using a leased lock to prevent double-execution.
     */
    @Async("workflowStepExecutor")
    public void executeStepAsync(String executionId, String stepName) {
        long startTime = System.currentTimeMillis();

        Optional<StepExecution> stepOpt = stepRepository.findByExecutionIdAndStepName(executionId, stepName);
        if (stepOpt.isEmpty()) {
            log.warn("Step not found: executionId={}, stepName={}", executionId, stepName);
            return;
        }

        StepExecution step = stepOpt.get();
        if (step.getStatus() == StepStatus.SUCCESS || step.getStatus() == StepStatus.COMPENSATED) {
            log.info("Step {} for execution {} is already in terminal state {}, skipping",
                    stepName, executionId, step.getStatus());
            return;
        }

        // Atomically claim lease in PostgreSQL / H2
        Instant now = Instant.now();
        Instant leaseExpiresAt = now.plusSeconds(leaseDurationSeconds);
        int claimed = stepRepository.claimStepLease(
                step.getId(),
                StepStatus.RUNNING,
                List.of(StepStatus.PENDING, StepStatus.RETRY_SCHEDULED),
                leaseExpiresAt,
                now
        );

        if (claimed == 0) {
            log.info("Failed to claim lease for step {} on execution {} (already claimed or in progress)",
                    stepName, executionId);
            return;
        }

        // Reload fresh step entity after claim
        step = stepRepository.findById(step.getId()).orElseThrow();
        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();

        eventService.logEvent(executionId, WorkflowEventType.STEP_STARTED, stepName,
                Map.of("attempt", step.getAttemptCount(), "leaseExpiresAt", leaseExpiresAt.toString()));

        Optional<Step> stepBeanOpt = stepRegistry.getStep(stepName);
        if (stepBeanOpt.isEmpty()) {
            handleStepFailure(executionId, step, "No bean implementation found for step: " + stepName);
            return;
        }

        Step stepBean = stepBeanOpt.get();
        StepContext context = StepContext.fromJson(executionId, stepName, step.getAttemptCount(), execution.getContextJson());

        try {
            StepResult result = stepBean.execute(context);
            long durationMs = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                handleStepSuccess(executionId, step, result, durationMs, context);
            } else if (result.isRetry()) {
                handleStepRetry(executionId, step, result.getErrorMessage(), result.getRetryDelay());
            } else {
                handleStepFailure(executionId, step, result.getErrorMessage());
            }
        } catch (Exception ex) {
            log.error("Unhandled exception in step {} for execution {}", stepName, executionId, ex);
            handleStepRetry(executionId, step, "Exception: " + ex.getMessage(), null);
        }
    }

    @Transactional
    public void handleStepSuccess(String executionId, StepExecution step, StepResult result, long durationMs, StepContext context) {
        Instant now = Instant.now();
        step.setStatus(StepStatus.SUCCESS);
        step.setCompletedAt(now);
        step.setDurationMs(durationMs);
        step.setError(null);
        step.setLeaseExpiresAt(null);
        stepRepository.save(step);

        // Update workflow execution context with output data
        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
        if (!result.getOutput().isEmpty()) {
            context.putAll(result.getOutput());
            execution.setContextJson(context.toJson());
        }

        eventService.logEvent(executionId, WorkflowEventType.STEP_COMPLETED, step.getStepName(),
                Map.of("durationMs", durationMs, "output", result.getOutput()));

        List<StepExecution> allSteps = stepRepository.findByExecutionIdOrderByStepIndexAsc(executionId);
        int nextIndex = step.getStepIndex() + 1;

        if (nextIndex < allSteps.size()) {
            StepExecution nextStep = allSteps.get(nextIndex);
            execution.setCurrentStepIndex(nextIndex);
            executionRepository.save(execution);
            log.info("Advancing workflow {} to next step: {}", executionId, nextStep.getStepName());
            executeStepAsync(executionId, nextStep.getStepName());
        } else {
            execution.setStatus(WorkflowStatus.COMPLETED);
            executionRepository.save(execution);
            eventService.logEvent(executionId, WorkflowEventType.WORKFLOW_COMPLETED, null,
                    Map.of("totalSteps", allSteps.size(), "completedAt", now.toString()));
            log.info("Workflow {} COMPLETED successfully!", executionId);
        }
    }

    @Transactional
    public void handleStepRetry(String executionId, StepExecution step, String reason, Duration customDelay) {
        int currentAttempt = step.getAttemptCount();
        int maxAttempts = step.getMaxAttempts();

        if (retryPolicy.canRetry(currentAttempt, maxAttempts)) {
            Duration delay = customDelay != null ? customDelay : retryPolicy.calculateDelay(currentAttempt);
            Instant nextRetryAt = Instant.now().plus(delay);

            step.setStatus(StepStatus.RETRY_SCHEDULED);
            step.setNextRetryAt(nextRetryAt);
            step.setLeaseExpiresAt(null);
            step.setError(reason);
            stepRepository.save(step);

            eventService.logEvent(executionId, WorkflowEventType.STEP_RETRY_SCHEDULED, step.getStepName(),
                    Map.of("attempt", currentAttempt, "maxAttempts", maxAttempts,
                           "nextRetryAt", nextRetryAt.toString(), "reason", reason));

            log.warn("Step {} failed (attempt {}/{}). Retry scheduled at {}: {}",
                    step.getStepName(), currentAttempt, maxAttempts, nextRetryAt, reason);
        } else {
            log.error("Step {} exhausted all {} attempts. Marking as FAILED: {}",
                    step.getStepName(), maxAttempts, reason);
            handleStepFailure(executionId, step, "Exhausted all retries (" + maxAttempts + "): " + reason);
        }
    }

    @Transactional
    public void handleStepFailure(String executionId, StepExecution step, String reason) {
        step.setStatus(StepStatus.FAILED);
        step.setCompletedAt(Instant.now());
        step.setError(reason);
        step.setLeaseExpiresAt(null);
        stepRepository.save(step);

        eventService.logEvent(executionId, WorkflowEventType.STEP_FAILED, step.getStepName(),
                Map.of("error", reason));

        // Trigger SAGA backward compensation
        compensateWorkflow(executionId, reason);
    }

    /**
     * SAGA Backward Compensation.
     * Reverses all previously completed steps in reverse order.
     */
    public void compensateWorkflow(String executionId, String failureReason) {
        log.warn("Initiating SAGA compensation for execution {}. Reason: {}", executionId, failureReason);

        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
        execution.setStatus(WorkflowStatus.COMPENSATING);
        executionRepository.save(execution);

        eventService.logEvent(executionId, WorkflowEventType.COMPENSATION_STARTED, null,
                Map.of("reason", failureReason));

        List<StepExecution> steps = stepRepository.findByExecutionIdOrderByStepIndexAsc(executionId);

        // Reverse iterate over steps that completed successfully
        for (int i = steps.size() - 1; i >= 0; i--) {
            StepExecution s = steps.get(i);
            if (s.getStatus() == StepStatus.SUCCESS) {
                log.info("Compensating step {} for execution {}", s.getStepName(), executionId);
                Optional<Step> stepBeanOpt = stepRegistry.getStep(s.getStepName());
                if (stepBeanOpt.isPresent()) {
                    Step stepBean = stepBeanOpt.get();
                    StepContext context = StepContext.fromJson(executionId, s.getStepName(), s.getAttemptCount(), execution.getContextJson());
                    try {
                        stepBean.compensate(context);
                        s.setStatus(StepStatus.COMPENSATED);
                        stepRepository.save(s);
                        eventService.logEvent(executionId, WorkflowEventType.STEP_COMPENSATED, s.getStepName(), null);
                    } catch (Exception ex) {
                        log.error("Compensation failed for step {}", s.getStepName(), ex);
                    }
                }
            }
        }

        execution.setStatus(WorkflowStatus.COMPENSATED);
        executionRepository.save(execution);

        eventService.logEvent(executionId, WorkflowEventType.WORKFLOW_FAILED, null,
                Map.of("finalStatus", "COMPENSATED", "failureReason", failureReason));
        log.warn("SAGA compensation completed for execution {}", executionId);
    }

    /**
     * Manually retries a failed step from the UI.
     */
    @Transactional
    public void manualRetry(String executionId, String stepName) {
        StepExecution step = stepRepository.findByExecutionIdAndStepName(executionId, stepName)
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepName));

        step.setStatus(StepStatus.PENDING);
        step.setNextRetryAt(null);
        step.setError(null);
        step.setLeaseExpiresAt(null);
        stepRepository.save(step);

        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
        if (execution.getStatus() != WorkflowStatus.RUNNING) {
            execution.setStatus(WorkflowStatus.RUNNING);
            executionRepository.save(execution);
        }

        eventService.logEvent(executionId, WorkflowEventType.STEP_RECOVERED, stepName,
                Map.of("trigger", "MANUAL_RETRY"));

        executeStepAsync(executionId, stepName);
    }

    /**
     * Cancels an active workflow execution.
     */
    @Transactional
    public void cancelWorkflow(String executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot cancel workflow in terminal state: " + execution.getStatus());
        }

        execution.setStatus(WorkflowStatus.CANCELLED);
        executionRepository.save(execution);

        eventService.logEvent(executionId, WorkflowEventType.WORKFLOW_CANCELLED, null,
                Map.of("cancelledAt", Instant.now().toString()));
    }

    /**
     * Simulates a worker crash (kill -9) on a running step.
     * Sets its lease as expired so the recovery scheduler rescues it.
     */
    @Transactional
    public boolean simulateCrash(String executionId) {
        List<StepExecution> steps = stepRepository.findByExecutionIdOrderByStepIndexAsc(executionId);
        for (StepExecution step : steps) {
            if (step.getStatus() == StepStatus.RUNNING) {
                // Set lease to expired 10 seconds ago
                step.setLeaseExpiresAt(Instant.now().minusSeconds(10));
                stepRepository.save(step);
                eventService.logEvent(executionId, WorkflowEventType.STEP_FAILED, step.getStepName(),
                        Map.of("simulatedCrash", true, "message", "Worker died (kill -9 simulation)"));
                log.warn("[SIMULATION] Worker crash simulated for step {} of execution {}", step.getStepName(), executionId);
                return true;
            }
        }
        return false;
    }

    private List<String> parseStepsJson(String stepsJson) {
        try {
            return OBJECT_MAPPER.readValue(stepsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
