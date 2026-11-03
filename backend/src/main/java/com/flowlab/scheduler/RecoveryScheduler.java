package com.flowlab.scheduler;

import com.flowlab.domain.StepExecution;
import com.flowlab.domain.StepStatus;
import com.flowlab.domain.WorkflowEventType;
import com.flowlab.engine.WorkflowEngine;
import com.flowlab.engine.WorkflowEventService;
import com.flowlab.repository.StepExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "flowlab.recovery.enabled", havingValue = "true", matchIfMissing = true)
public class RecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecoveryScheduler.class);

    private final StepExecutionRepository stepRepository;
    private final WorkflowEngine workflowEngine;
    private final WorkflowEventService eventService;

    public RecoveryScheduler(
            StepExecutionRepository stepRepository,
            WorkflowEngine workflowEngine,
            WorkflowEventService eventService
    ) {
        this.stepRepository = stepRepository;
        this.workflowEngine = workflowEngine;
        this.eventService = eventService;
    }

    /**
     * Periodically scans for stuck steps (dead workers) and due scheduled retries.
     */
    @Scheduled(fixedDelayString = "${flowlab.recovery.interval-ms:3000}")
    @Transactional
    public void runRecoveryCycle() {
        recoverStuckSteps();
        dispatchDueRetries();
    }

    /**
     * Rescues steps stuck in RUNNING state whose lease expired.
     * This occurs when an execution instance or worker process was terminated (kill -9).
     */
    private void recoverStuckSteps() {
        Instant now = Instant.now();
        List<StepExecution> stuckSteps = stepRepository.findStuckRunningSteps(StepStatus.RUNNING, now);

        for (StepExecution step : stuckSteps) {
            String executionId = step.getExecution().getId();
            log.warn("[CRASH RECOVERY] Detected dead worker lease for executionId={}, step={}. Recovering...",
                    executionId, step.getStepName());

            // Reset step status to RETRY_SCHEDULED so it can be re-executed safely
            step.setStatus(StepStatus.RETRY_SCHEDULED);
            step.setNextRetryAt(now);
            step.setLeaseExpiresAt(null);
            stepRepository.save(step);

            eventService.logEvent(executionId, WorkflowEventType.STEP_RECOVERED, step.getStepName(),
                    Map.of("reason", "Worker lease expired. Automatic crash recovery triggered.",
                           "recoveredAt", now.toString()));

            workflowEngine.executeStepAsync(executionId, step.getStepName());
        }
    }

    /**
     * Dispatches steps whose exponential backoff retry timestamp is due.
     */
    private void dispatchDueRetries() {
        Instant now = Instant.now();
        List<StepExecution> dueSteps = stepRepository.findDueRetrySteps(StepStatus.RETRY_SCHEDULED, now);

        for (StepExecution step : dueSteps) {
            String executionId = step.getExecution().getId();
            log.info("[RETRY DISPATCH] Retrying due step {} for executionId={}, attempt={}",
                    step.getStepName(), executionId, step.getAttemptCount() + 1);

            step.setNextRetryAt(null);
            stepRepository.save(step);

            workflowEngine.executeStepAsync(executionId, step.getStepName());
        }
    }
}
