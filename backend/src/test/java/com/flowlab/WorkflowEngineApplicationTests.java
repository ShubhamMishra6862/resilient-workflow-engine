package com.flowlab;

import com.flowlab.domain.StepStatus;
import com.flowlab.domain.WorkflowExecution;
import com.flowlab.domain.WorkflowStatus;
import com.flowlab.engine.IdempotencyService;
import com.flowlab.engine.StepContext;
import com.flowlab.engine.StepResult;
import com.flowlab.engine.WorkflowEngine;
import com.flowlab.repository.StepExecutionRepository;
import com.flowlab.repository.WorkflowExecutionRepository;
import com.flowlab.steps.ProcessPaymentStep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorkflowEngineApplicationTests {

    @Autowired
    private WorkflowEngine workflowEngine;

    @Autowired
    private WorkflowExecutionRepository executionRepository;

    @Autowired
    private StepExecutionRepository stepRepository;

    @Autowired
    private ProcessPaymentStep paymentStep;

    @Test
    void contextLoads() {
        assertNotNull(workflowEngine);
    }

    @Test
    void testStartOrderWorkflow() throws InterruptedException {
        Map<String, Object> input = Map.of(
                "orderId", "ORD-TEST-101",
                "customerId", "CUST-99",
                "amount", 199.99
        );

        WorkflowExecution execution = workflowEngine.startWorkflow("order-processing", input);
        assertNotNull(execution);
        assertNotNull(execution.getId());
        assertEquals("order-processing", execution.getWorkflowName());
        assertEquals(WorkflowStatus.RUNNING, execution.getStatus());

        // Wait a few seconds for steps to progress
        Thread.sleep(4000);

        WorkflowExecution updated = executionRepository.findById(execution.getId()).orElseThrow();
        assertNotNull(updated);
        assertTrue(updated.getSteps().size() >= 4);
    }

    @Test
    void testPaymentIdempotency() throws Exception {
        String executionId = "test-idempotency-exec-1";
        StepContext ctx1 = new StepContext(executionId, "ProcessPayment", 1, Map.of("amount", 250.0));

        StepResult res1 = paymentStep.execute(ctx1);
        assertTrue(res1.isSuccess());
        String paymentId1 = (String) res1.getOutput().get("paymentId");
        assertNotNull(paymentId1);

        // Re-execute with same executionId
        StepContext ctx2 = new StepContext(executionId, "ProcessPayment", 2, Map.of("amount", 250.0));
        StepResult res2 = paymentStep.execute(ctx2);
        assertTrue(res2.isSuccess());
        String paymentId2 = (String) res2.getOutput().get("paymentId");

        // Must return the exact same paymentId without double charging
        assertEquals(paymentId1, paymentId2);
        assertTrue((Boolean) res2.getOutput().get("idempotencyHit"));
    }
}
