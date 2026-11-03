package com.flowlab.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StepContext {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String executionId;
    private final String stepName;
    private final int attemptCount;
    private final Map<String, Object> data;

    public StepContext(String executionId, String stepName, int attemptCount, Map<String, Object> data) {
        this.executionId = executionId;
        this.stepName = stepName;
        this.attemptCount = attemptCount;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }

    public static StepContext fromJson(String executionId, String stepName, int attemptCount, String json) {
        if (json == null || json.trim().isEmpty() || json.equals("{}")) {
            return new StepContext(executionId, stepName, attemptCount, new HashMap<>());
        }
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            return new StepContext(executionId, stepName, attemptCount, map);
        } catch (Exception e) {
            return new StepContext(executionId, stepName, attemptCount, new HashMap<>());
        }
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this.data);
        } catch (Exception e) {
            return "{}";
        }
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getStepName() {
        return stepName;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public String getString(String key) {
        Object val = data.get(key);
        return val != null ? val.toString() : null;
    }

    public Double getDouble(String key) {
        Object val = data.get(key);
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception ignored) {}
        }
        return null;
    }

    public Integer getInteger(String key) {
        Object val = data.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        return null;
    }

    public Boolean getBoolean(String key, boolean defaultValue) {
        Object val = data.get(key);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return defaultValue;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public void putAll(Map<String, Object> entries) {
        if (entries != null) {
            data.putAll(entries);
        }
    }
}
