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
public class UpdateInventoryStep implements Step {

    private static final Logger log = LoggerFactory.getLogger(UpdateInventoryStep.class);

    @Override
    public String getName() {
        return "UpdateInventory";
    }

    @Override
    public StepResult execute(StepContext context) throws Exception {
        log.info("[UpdateInventory] Updating stock for executionId={}, attempt={}",
                context.getExecutionId(), context.getAttemptCount());

        // Simulate processing duration
        Thread.sleep(1000);

        boolean simulateFailure = context.getBoolean("simulateFailure", false);
        int failAttempts = context.getInteger("failAttempts") != null ? context.getInteger("failAttempts") : 1;
        boolean failPermanently = context.getBoolean("failPermanently", false);

        if (failPermanently) {
            log.warn("[UpdateInventory] Permanent failure triggered for demonstration!");
            return StepResult.fail("Out of stock: SKU-7740 is exhausted in all regional warehouses");
        }

        if (simulateFailure && context.getAttemptCount() <= failAttempts) {
            log.warn("[UpdateInventory] Simulated transient failure on attempt {}/{}",
                    context.getAttemptCount(), failAttempts);
            return StepResult.retry("Warehouse inventory sync timed out (attempt " + context.getAttemptCount() + ")");
        }

        Map<String, Object> output = new HashMap<>();
        output.put("inventoryReserved", true);
        output.put("warehouseId", "WH-EAST-04");
        output.put("reservedAt", Instant.now().toString());

        log.info("[UpdateInventory] Inventory reserved successfully");
        return StepResult.success(output);
    }

    @Override
    public void compensate(StepContext context) throws Exception {
        log.info("[UpdateInventory SAGA] Compensating: Releasing reserved stock for executionId={}", context.getExecutionId());
        Thread.sleep(500);
        context.put("inventoryReleased", true);
        context.put("releasedAt", Instant.now().toString());
    }
}
