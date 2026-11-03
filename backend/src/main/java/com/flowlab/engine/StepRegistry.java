package com.flowlab.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StepRegistry {

    private static final Logger log = LoggerFactory.getLogger(StepRegistry.class);
    private final Map<String, Step> steps = new ConcurrentHashMap<>();

    public StepRegistry(List<Step> stepBeans) {
        if (stepBeans != null) {
            for (Step step : stepBeans) {
                steps.put(step.getName(), step);
                log.info("Registered workflow step: '{}' -> {}", step.getName(), step.getClass().getSimpleName());
            }
        }
    }

    public Optional<Step> getStep(String stepName) {
        return Optional.ofNullable(steps.get(stepName));
    }

    public boolean hasStep(String stepName) {
        return steps.containsKey(stepName);
    }
}
