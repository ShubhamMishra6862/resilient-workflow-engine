package com.flowlab.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_execution")
public class WorkflowExecution {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "workflow_name", nullable = false, length = 100)
    private String workflowName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id")
    private WorkflowDefinition definition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowStatus status = WorkflowStatus.RUNNING;

    @Column(name = "current_step_index", nullable = false)
    private Integer currentStepIndex = 0;

    @Column(name = "context_json", nullable = false, columnDefinition = "TEXT")
    private String contextJson = "{}";

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepIndex ASC")
    private List<StepExecution> steps = new ArrayList<>();

    public WorkflowExecution() {}

    public WorkflowExecution(String id, String workflowName, WorkflowDefinition definition, String contextJson) {
        this.id = id;
        this.workflowName = workflowName;
        this.definition = definition;
        this.contextJson = contextJson != null ? contextJson : "{}";
        this.status = WorkflowStatus.RUNNING;
        this.currentStepIndex = 0;
        this.version = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addStep(StepExecution step) {
        this.steps.add(step);
        step.setExecution(this);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public WorkflowDefinition getDefinition() { return definition; }
    public void setDefinition(WorkflowDefinition definition) { this.definition = definition; }

    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }

    public Integer getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(Integer currentStepIndex) { this.currentStepIndex = currentStepIndex; }

    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<StepExecution> getSteps() { return steps; }
    public void setSteps(List<StepExecution> steps) { this.steps = steps; }
}
