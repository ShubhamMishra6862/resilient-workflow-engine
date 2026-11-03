package com.flowlab.steps;

import com.flowlab.engine.Step;
import com.flowlab.engine.StepContext;
import com.flowlab.engine.StepResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class RestockInventoryStep implements Step {

    @Override
    public String getName() {
        return "RestockInventory";
    }

    @Override
    public StepResult execute(StepContext context) throws Exception {
        Thread.sleep(700);
        Map<String, Object> output = new HashMap<>();
        output.put("itemsRestocked", true);
        output.put("restockedAt", Instant.now().toString());
        return StepResult.success(output);
    }
}
