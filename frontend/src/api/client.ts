import { WorkflowDefinition, WorkflowExecution, WorkflowEvent, StartWorkflowParams } from '../types';

const API_BASE = '/api/v1';

export async function fetchWorkflows(): Promise<WorkflowDefinition[]> {
  const res = await fetch(`${API_BASE}/workflows`);
  if (!res.ok) throw new Error('Failed to fetch workflows');
  return res.json();
}

export async function fetchWorkflow(name: string): Promise<WorkflowDefinition> {
  const res = await fetch(`${API_BASE}/workflows/${name}`);
  if (!res.ok) throw new Error(`Failed to fetch workflow ${name}`);
  return res.json();
}

export async function fetchExecutions(workflowName?: string): Promise<WorkflowExecution[]> {
  const url = new URL(`${window.location.origin}${API_BASE}/executions`);
  if (workflowName) url.searchParams.append('workflowName', workflowName);
  url.searchParams.append('size', '20');

  const res = await fetch(url.toString());
  if (!res.ok) throw new Error('Failed to fetch executions');
  const data = await res.json();
  return data.content || [];
}

export async function fetchExecution(id: string): Promise<WorkflowExecution> {
  const res = await fetch(`${API_BASE}/executions/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch execution ${id}`);
  return res.json();
}

export async function fetchEvents(id: string): Promise<WorkflowEvent[]> {
  const res = await fetch(`${API_BASE}/executions/${id}/events`);
  if (!res.ok) throw new Error(`Failed to fetch events for ${id}`);
  return res.json();
}

export async function startWorkflow(workflowName: string, params: StartWorkflowParams): Promise<WorkflowExecution> {
  const res = await fetch(`${API_BASE}/workflows/${workflowName}/executions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ inputData: params })
  });
  if (!res.ok) throw new Error('Failed to start workflow');
  return res.json();
}

export async function retryStep(executionId: string, stepName: string): Promise<void> {
  const res = await fetch(`${API_BASE}/executions/${executionId}/steps/${stepName}/retry`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error(`Failed to retry step ${stepName}`);
}

export async function cancelWorkflow(executionId: string): Promise<void> {
  const res = await fetch(`${API_BASE}/executions/${executionId}/cancel`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error('Failed to cancel workflow');
}

export async function simulateCrash(executionId: string): Promise<{ success: boolean; message: string }> {
  const res = await fetch(`${API_BASE}/executions/${executionId}/simulate-crash`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error('Failed to simulate crash');
  return res.json();
}
