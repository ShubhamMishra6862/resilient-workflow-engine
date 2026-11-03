export type WorkflowStatus =
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'COMPENSATING'
  | 'COMPENSATED'
  | 'CANCELLED';

export type StepStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'RETRY_SCHEDULED'
  | 'FAILED'
  | 'COMPENSATED';

export interface StepExecution {
  id: string;
  stepName: string;
  stepIndex: number;
  status: StepStatus;
  attemptCount: number;
  maxAttempts: number;
  nextRetryAt: string | null;
  leaseExpiresAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  durationMs: number | null;
  error: string | null;
}

export interface WorkflowExecution {
  id: string;
  workflowName: string;
  status: WorkflowStatus;
  currentStepIndex: number;
  context: Record<string, any>;
  version: number;
  createdAt: string;
  updatedAt: string;
  steps: StepExecution[];
}

export interface WorkflowDefinition {
  id: number;
  name: string;
  version: number;
  description: string;
  steps: string[];
  active: boolean;
  createdAt: string;
}

export interface WorkflowEvent {
  id: number;
  executionId: string;
  eventType: string;
  stepName: string | null;
  detailJson: string | null;
  createdAt: string;
}

export interface StartWorkflowParams {
  orderId?: string;
  customerId?: string;
  amount?: number;
  customerEmail?: string;
  simulateFailure?: boolean;
  failAttempts?: number;
  failPermanently?: boolean;
}
