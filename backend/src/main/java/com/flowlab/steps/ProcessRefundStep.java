package com.flowlab.steps;

import com.flowlab.engine.Step;
import com.flowlab.engine.StepContext;
import com.flowlab.engine.StepResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ProcessRefundStep implements Step {

    @Override
    public String getName() {
        return "ProcessRefund";
    }

    @Override
    public StepResult execute(StepContext context) throws Exception {
        Thread.sleep(800);
        String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> output = new HashMap<>();
        output.put("refundId", refundId);
        output.put("refundStatus", "SUCCESS");
        output.put("refundedAt", Instant.now().toString());
        return StepResult.success(output);
    }
}
