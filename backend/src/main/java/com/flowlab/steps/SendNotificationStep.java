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
public class SendNotificationStep implements Step {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationStep.class);

    @Override
    public String getName() {
        return "SendNotification";
    }

    @Override
    public StepResult execute(StepContext context) throws Exception {
        log.info("[SendNotification] Sending order confirmation for executionId={}", context.getExecutionId());

        Thread.sleep(700);

        String customerEmail = context.getString("customerEmail");
        if (customerEmail == null) {
            customerEmail = "customer@example.com";
        }

        Map<String, Object> output = new HashMap<>();
        output.put("notificationSent", true);
        output.put("recipient", customerEmail);
        output.put("channel", "EMAIL_AND_SMS");
        output.put("sentAt", Instant.now().toString());

        log.info("[SendNotification] Dispatched customer confirmation to {}", customerEmail);
        return StepResult.success(output);
    }

    @Override
    public void compensate(StepContext context) throws Exception {
        log.info("[SendNotification SAGA] Sending cancellation notice to customer for executionId={}", context.getExecutionId());
    }
}
