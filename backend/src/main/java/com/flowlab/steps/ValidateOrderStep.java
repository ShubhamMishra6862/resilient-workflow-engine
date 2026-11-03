package com.flowlab.steps;

import com.flowlab.engine.Step;
import com.flowlab.engine.StepContext;
import com.flowlab.engine.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class ValidateOrderStep implements Step {

    private static final Logger log = LoggerFactory.getLogger(ValidateOrderStep.class);

    @Override
    public String getName() {
        return "ValidateOrder";
    }

    @Override
    public StepResult execute(StepContext context) throws Exception {
        log.info("[ValidateOrder] Validating order for executionId={}", context.getExecutionId());

        // Simulate realistic processing time
        Thread.sleep(800);

        String orderId = context.getString("orderId");
        Double amount = context.getDouble("amount");

        if (orderId == null || orderId.trim().isEmpty()) {
            orderId = "ORD-" + System.currentTimeMillis() % 100000;
        }

        if (amount != null && amount <= 0) {
            return StepResult.fail("Validation failed: order amount must be greater than zero");
        }

        Map<String, Object> output = new HashMap<>();
        output.put("orderId", orderId);
        output.put("orderValidated", true);
        output.put("validatedAt", Instant.now().toString());

        log.info("[ValidateOrder] Successfully validated orderId={}", orderId);
        return StepResult.success(output);
    }

    @Override
    public void compensate(StepContext context) throws Exception {
        log.info("[ValidateOrder] Compensation: Voiding order validation for executionId={}", context.getExecutionId());
    }
}
