package com.flowlab.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workflow_event")
public class WorkflowEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, length = 36)
    private String executionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private WorkflowEventType eventType;

    @Column(name = "step_name", length = 100)
    private String stepName;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WorkflowEvent() {}

    public WorkflowEvent(String executionId, WorkflowEventType eventType, String stepName, String detailJson) {
        this.executionId = executionId;
        this.eventType = eventType;
        this.stepName = stepName;
        this.detailJson = detailJson;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public WorkflowEventType getEventType() { return eventType; }
    public void setEventType(WorkflowEventType eventType) { this.eventType = eventType; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
