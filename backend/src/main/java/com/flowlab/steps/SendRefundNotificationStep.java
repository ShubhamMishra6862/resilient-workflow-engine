package com.flowlab.steps;

import com.flowlab.engine.Step;
import com.flowlab.engine.StepContext;
import com.flowlab.engine.StepResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class SendRefundNotificationStep implements Step {

    @Override
    public String getName() {
        return "SendRefundNotification";
    }

    @Override
    public StepResult execute(StepContext context) throws Exception {
        Thread.sleep(600);
        Map<String, Object> output = new HashMap<>();
        output.put("refundNotificationSent", true);
        output.put("sentAt", Instant.now().toString());
        return StepResult.success(output);
    }
}
