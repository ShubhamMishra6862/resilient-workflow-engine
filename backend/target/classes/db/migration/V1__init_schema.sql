-- ==============================================================================
-- FlowLab Workflow Engine Database Schema Migration
-- V1: Core Tables, Unique Constraints, Polling Indexes, and Initial Blueprints
-- ==============================================================================

-- 1. Workflow Definition: Blueprints for workflow execution templates
CREATE TABLE IF NOT EXISTS workflow_definition (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    description TEXT,
    steps_json TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workflow_name_version UNIQUE (name, version)
);

-- 2. Workflow Execution: Parent record tracking overall execution lifecycle
CREATE TABLE IF NOT EXISTS workflow_execution (
    id VARCHAR(36) PRIMARY KEY,
    workflow_name VARCHAR(100) NOT NULL,
    definition_id BIGINT REFERENCES workflow_definition(id),
    status VARCHAR(30) NOT NULL,
    current_step_index INT NOT NULL DEFAULT 0,
    context_json TEXT NOT NULL DEFAULT '{}',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Step Execution: Child record tracking each step's attempt and lease status
CREATE TABLE IF NOT EXISTS step_execution (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL REFERENCES workflow_execution(id) ON DELETE CASCADE,
    step_name VARCHAR(100) NOT NULL,
    step_index INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE NULL,
    lease_expires_at TIMESTAMP WITH TIME ZONE NULL,
    started_at TIMESTAMP WITH TIME ZONE NULL,
    completed_at TIMESTAMP WITH TIME ZONE NULL,
    duration_ms BIGINT NULL,
    error TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_execution_step UNIQUE (execution_id, step_name)
);

-- 4. Workflow Event: Append-only audit trail and live event log
CREATE TABLE IF NOT EXISTS workflow_event (
    id BIGSERIAL PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL REFERENCES workflow_execution(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    step_name VARCHAR(100) NULL,
    detail_json TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Idempotency Record: Ensures operations with side-effects are executed effectively once
CREATE TABLE IF NOT EXISTS idempotency_record (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    result_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Outbox: Transactional outbox table for reliable asynchronous event publication
CREATE TABLE IF NOT EXISTS outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE NULL
);

-- ==============================================================================
-- Polling & Query Optimization Indexes
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_workflow_exec_status ON workflow_execution(status, updated_at);
CREATE INDEX IF NOT EXISTS idx_workflow_exec_name ON workflow_execution(workflow_name);

CREATE INDEX IF NOT EXISTS idx_step_exec_recovery_due ON step_execution(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_step_exec_recovery_lease ON step_execution(status, lease_expires_at);
CREATE INDEX IF NOT EXISTS idx_step_exec_parent ON step_execution(execution_id, step_index);

CREATE INDEX IF NOT EXISTS idx_workflow_event_exec_time ON workflow_event(execution_id, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_poll ON outbox(status, created_at);

-- ==============================================================================
-- Initial Blueprint Seeds
-- ==============================================================================
INSERT INTO workflow_definition (name, version, description, steps_json, active)
VALUES (
    'order-processing',
    1,
    'Process customer orders with payment, inventory and notification steps.',
    '["ValidateOrder","ProcessPayment","UpdateInventory","SendNotification"]',
    true
) ON CONFLICT (name, version) DO NOTHING;

INSERT INTO workflow_definition (name, version, description, steps_json, active)
VALUES (
    'refund-processing',
    1,
    'Handle order refund, inventory restocking and notification.',
    '["ValidateRefund","ProcessRefund","RestockInventory","SendRefundNotification"]',
    true
) ON CONFLICT (name, version) DO NOTHING;
