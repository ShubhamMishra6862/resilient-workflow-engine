import React, { useState, useEffect, useCallback } from 'react';
import { Sidebar } from './components/Sidebar';
import { Header } from './components/Header';
import { PipelineView } from './components/PipelineView';
import { ExecutionHistoryTable } from './components/ExecutionHistoryTable';
import { StartWorkflowModal } from './components/StartWorkflowModal';
import { LogsDrawer } from './components/LogsDrawer';
import {
  WorkflowDefinition,
  WorkflowExecution,
  WorkflowEvent,
  StartWorkflowParams,
  StepExecution,
} from './types';
import * as api from './api/client';
import { ShieldCheck, Info } from 'lucide-react';

// Default initial visual state matching the Instagram post screenshot exactly
const DEFAULT_DEMO_STEPS: StepExecution[] = [
  {
    id: 's-1',
    stepName: 'ValidateOrder',
    stepIndex: 0,
    status: 'SUCCESS',
    attemptCount: 1,
    maxAttempts: 3,
    nextRetryAt: null,
    leaseExpiresAt: null,
    startedAt: '2024-10-10T10:24:11Z',
    completedAt: '2024-10-10T10:24:13.3Z',
    durationMs: 2300,
    error: null,
  },
  {
    id: 's-2',
    stepName: 'ProcessPayment',
    stepIndex: 1,
    status: 'SUCCESS',
    attemptCount: 1,
    maxAttempts: 3,
    nextRetryAt: null,
    leaseExpiresAt: null,
    startedAt: '2024-10-10T10:24:14Z',
    completedAt: '2024-10-10T10:24:18.1Z',
    durationMs: 4100,
    error: null,
  },
  {
    id: 's-3',
    stepName: 'UpdateInventory',
    stepIndex: 2,
    status: 'RUNNING',
    attemptCount: 2,
    maxAttempts: 3,
    nextRetryAt: null,
    leaseExpiresAt: new Date(Date.now() + 30000).toISOString(),
    startedAt: '2024-10-10T10:24:18Z',
    completedAt: null,
    durationMs: 12400,
    error: 'Warehouse sync timed out (attempt 1)',
  },
  {
    id: 's-4',
    stepName: 'SendNotification',
    stepIndex: 3,
    status: 'PENDING',
    attemptCount: 0,
    maxAttempts: 3,
    nextRetryAt: null,
    leaseExpiresAt: null,
    startedAt: null,
    completedAt: null,
    durationMs: null,
    error: null,
  },
];

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState('workflows');
  const [workflows, setWorkflows] = useState<WorkflowDefinition[]>([]);
  const [selectedWorkflowName, setSelectedWorkflowName] = useState('order-processing');
  const [activeWorkflow, setActiveWorkflow] = useState<WorkflowDefinition | null>(null);

  const [activeExecution, setActiveExecution] = useState<WorkflowExecution | null>(null);
  const [displaySteps, setDisplaySteps] = useState<StepExecution[]>(DEFAULT_DEMO_STEPS);
  const [events, setEvents] = useState<WorkflowEvent[]>([]);

  const [isStartModalOpen, setIsStartModalOpen] = useState(false);
  const [isLogsDrawerOpen, setIsLogsDrawerOpen] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [isSimulatingCrash, setIsSimulatingCrash] = useState(false);
  const [notification, setNotification] = useState<string | null>(null);

  const showNotification = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 4500);
  };

  // 1. Fetch available workflow definitions on mount
  useEffect(() => {
    const loadWorkflows = async () => {
      try {
        const list = await api.fetchWorkflows();
        if (list && list.length > 0) {
          setWorkflows(list);
          const found = list.find((w) => w.name === selectedWorkflowName) || list[0];
          setActiveWorkflow(found);
          setSelectedWorkflowName(found.name);
        }
      } catch (err) {
        console.warn('Backend not yet reachable, rendering in preview mode', err);
      }
    };
    loadWorkflows();
  }, [selectedWorkflowName]);

  // 2. Poll active execution telemetry
  const refreshActiveExecution = useCallback(async () => {
    if (!activeExecution?.id) return;
    try {
      const refreshed = await api.fetchExecution(activeExecution.id);
      setActiveExecution(refreshed);
      if (refreshed.steps && refreshed.steps.length > 0) {
        setDisplaySteps(refreshed.steps);
      }
    } catch (err) {
      console.error('Error refreshing execution', err);
    }
  }, [activeExecution?.id]);

  useEffect(() => {
    if (!autoRefresh || !activeExecution?.id) return;
    const interval = setInterval(() => {
      refreshActiveExecution();
    }, 1500);
    return () => clearInterval(interval);
  }, [autoRefresh, activeExecution?.id, refreshActiveExecution]);

  // Handle workflow definition change
  const handleSelectWorkflow = (name: string) => {
    setSelectedWorkflowName(name);
    const found = workflows.find((w) => w.name === name);
    if (found) {
      setActiveWorkflow(found);
    }
  };

  // Launch a new workflow
  const handleStartWorkflow = async (params: StartWorkflowParams) => {
    try {
      showNotification('Launching resilient workflow execution...');
      const created = await api.startWorkflow(selectedWorkflowName, params);
      setActiveExecution(created);
      if (created.steps && created.steps.length > 0) {
        setDisplaySteps(created.steps);
      }
      showNotification(`Execution started: ${created.id.substring(0, 8)}`);
    } catch (err: any) {
      showNotification(`Failed to start: ${err.message}`);
    }
  };

  // Open audit logs drawer
  const handleOpenLogs = async () => {
    if (activeExecution?.id) {
      try {
        const evts = await api.fetchEvents(activeExecution.id);
        setEvents(evts);
      } catch (err) {
        console.error('Error fetching events', err);
      }
    }
    setIsLogsDrawerOpen(true);
  };

  // Manual retry of a failed step
  const handleRetryStep = async (stepName: string) => {
    if (!activeExecution?.id) return;
    try {
      await api.retryStep(activeExecution.id, stepName);
      showNotification(`Manually dispatched retry for ${stepName}`);
      refreshActiveExecution();
    } catch (err: any) {
      showNotification(`Retry failed: ${err.message}`);
    }
  };

  // Simulate Crash (kill -9) mid-step
  const handleSimulateCrash = async () => {
    if (!activeExecution?.id) {
      showNotification('Start a live workflow first to simulate worker crash');
      return;
    }
    setIsSimulatingCrash(true);
    try {
      const res = await api.simulateCrash(activeExecution.id);
      showNotification(res.message);
      refreshActiveExecution();
    } catch (err: any) {
      showNotification(`Crash simulation failed: ${err.message}`);
    } finally {
      setIsSimulatingCrash(false);
    }
  };

  return (
    <div className="flex h-screen bg-[#0c1017] text-slate-100 antialiased overflow-hidden">
      {/* Left Navigation Sidebar */}
      <Sidebar currentTab={currentTab} onSelectTab={setCurrentTab} />

      {/* Main Content Viewport */}
      <main className="flex-1 overflow-y-auto px-8 py-6">
        {/* Floating Notification */}
        {notification && (
          <div className="fixed top-5 right-6 z-50 flex items-center gap-2.5 px-4 py-2.5 rounded-xl bg-slate-900 border border-blue-500/40 text-blue-300 text-xs shadow-2xl animate-in slide-in-from-top-3">
            <Info className="w-4 h-4 text-blue-400" />
            <span>{notification}</span>
          </div>
        )}

        {/* Workflow Header matching Instagram screenshot */}
        <Header
          workflow={activeWorkflow}
          workflows={workflows}
          selectedWorkflowName={selectedWorkflowName}
          onSelectWorkflow={handleSelectWorkflow}
          onOpenStartModal={() => setIsStartModalOpen(true)}
          onSimulateCrash={handleSimulateCrash}
          isSimulatingCrash={isSimulatingCrash}
          autoRefresh={autoRefresh}
          onToggleAutoRefresh={() => setAutoRefresh(!autoRefresh)}
          latestExecution={activeExecution}
        />

        {/* Horizontal Step Pipeline Visualization */}
        <PipelineView
          steps={displaySteps}
          currentStepIndex={activeExecution?.currentStepIndex ?? 2}
        />

        {/* Execution History Table matching Instagram screenshot */}
        <ExecutionHistoryTable
          steps={displaySteps}
          onOpenLogs={handleOpenLogs}
          onRetryStep={handleRetryStep}
        />

        {/* System Design Highlights Footer */}
        <div className="mt-8 p-4 rounded-xl bg-slate-900/40 border border-slate-800/80 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 text-xs text-slate-400">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400">
              <ShieldCheck className="w-4 h-4" />
            </div>
            <div>
              <span className="font-semibold text-slate-200">Crash-Proof Guarantees: </span>
              Durable PostgreSQL State Transitions • Idempotency Keys (no double-charging) • Non-blocking Exponential Backoff • SAGA Backward Compensation
            </div>
          </div>
          <div className="font-mono text-slate-500 text-[11px]">
            {activeExecution?.id ? `Execution: ${activeExecution.id.substring(0, 13)}...` : 'Demo Mode Preview'}
          </div>
        </div>
      </main>

      {/* Start Workflow Modal */}
      <StartWorkflowModal
        isOpen={isStartModalOpen}
        onClose={() => setIsStartModalOpen(false)}
        onSubmit={handleStartWorkflow}
        workflowName={selectedWorkflowName}
      />

      {/* Event Logs Stream Slide-over Drawer */}
      <LogsDrawer
        isOpen={isLogsDrawerOpen}
        onClose={() => setIsLogsDrawerOpen(false)}
        events={events}
        executionId={activeExecution?.id || 'demo-run'}
      />
    </div>
  );
};
export default App;
