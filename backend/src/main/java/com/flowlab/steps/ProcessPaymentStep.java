package com.flowlab.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowlab.engine.IdempotencyService;
import com.flowlab.engine.Step;
import com.flowlab.engine.StepContext;
import com.flowlab.engine.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProcessPaymentStep implements Step {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentStep.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IdempotencyService idempotencyService;

    public ProcessPaymentStep(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    public String getName() {
        return "ProcessPayment";
    }

    @Override
    public StepResult execute(StepContext context) throws Exception {
        String executionId = context.getExecutionId();
        String idempotencyKey = IdempotencyService.buildKey(executionId, getName());

        log.info("[ProcessPayment] Checking idempotency for key={}", idempotencyKey);
        Optional<String> cachedResult = idempotencyService.getResult(idempotencyKey);
        if (cachedResult.isPresent()) {
            log.info("[ProcessPayment] Idempotency record FOUND! Skipping payment gateway call: {}", cachedResult.get());
            @SuppressWarnings("unchecked")
            Map<String, Object> output = OBJECT_MAPPER.readValue(cachedResult.get(), Map.class);
            output.put("idempotencyHit", true);
            return StepResult.success(output);
        }

        // Simulate gateway network latency
        Thread.sleep(1200);

        Double amount = context.getDouble("amount");
        if (amount == null) {
            amount = 149.99;
        }

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Map<String, Object> output = new HashMap<>();
        output.put("paymentId", paymentId);
        output.put("paidAmount", amount);
        output.put("paymentStatus", "SETTLED");
        output.put("paidAt", Instant.now().toString());

        // Save idempotency record atomically so repeated attempts won't double-charge
        idempotencyService.saveResult(
                idempotencyKey,
                executionId,
                getName(),
                OBJECT_MAPPER.writeValueAsString(output)
        );

        log.info("[ProcessPayment] Successfully charged amount={} paymentId={}", amount, paymentId);
        return StepResult.success(output);
    }

    @Override
    public void compensate(StepContext context) throws Exception {
        String paymentId = context.getString("paymentId");
        log.info("[ProcessPayment SAGA] Compensating: Refunding paymentId={} for executionId={}", paymentId, context.getExecutionId());
        Thread.sleep(600);
        context.put("paymentRefunded", true);
        context.put("refundedAt", Instant.now().toString());
    }
}
