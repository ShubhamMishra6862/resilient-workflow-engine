package com.flowlab.api.dto;

import java.util.HashMap;
import java.util.Map;

public class StartWorkflowRequest {

    private Map<String, Object> inputData = new HashMap<>();

    public StartWorkflowRequest() {}

    public StartWorkflowRequest(Map<String, Object> inputData) {
        this.inputData = inputData != null ? inputData : new HashMap<>();
    }

    public Map<String, Object> getInputData() {
        return inputData;
    }

    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }
}
