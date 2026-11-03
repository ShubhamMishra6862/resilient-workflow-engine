import React from 'react';
import { Play, RefreshCw, ZapOff, ChevronRight } from 'lucide-react';
import { WorkflowDefinition, WorkflowExecution } from '../types';

interface HeaderProps {
  workflow: WorkflowDefinition | null;
  workflows: WorkflowDefinition[];
  selectedWorkflowName: string;
  onSelectWorkflow: (name: string) => void;
  onOpenStartModal: () => void;
  onSimulateCrash: () => void;
  isSimulatingCrash: boolean;
  autoRefresh: boolean;
  onToggleAutoRefresh: () => void;
  latestExecution: WorkflowExecution | null;
}

export const Header: React.FC<HeaderProps> = ({
  workflow,
  workflows,
  selectedWorkflowName,
  onSelectWorkflow,
  onOpenStartModal,
  onSimulateCrash,
  isSimulatingCrash,
  autoRefresh,
  onToggleAutoRefresh,
  latestExecution,
}) => {
  const isRunning = latestExecution?.status === 'RUNNING';

  return (
    <div className="pb-6 border-b border-slate-800/80 mb-6">
      {/* Breadcrumb Navigation */}
      <div className="flex items-center gap-2 text-xs font-medium text-slate-400 mb-3">
        <span>Workflows</span>
        <ChevronRight className="w-3.5 h-3.5 text-slate-600" />
        <span className="text-slate-200">{workflow?.name || selectedWorkflowName}</span>
      </div>

      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Title and Badge */}
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-white tracking-tight capitalize">
              {workflow?.name?.replace('-', ' ') || 'Order Processing'}
            </h1>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
              Active
            </span>

            {/* Workflow Blueprint Switcher */}
            {workflows.length > 1 && (
              <select
                value={selectedWorkflowName}
                onChange={(e) => onSelectWorkflow(e.target.value)}
                className="bg-slate-900 border border-slate-700 text-xs text-slate-300 rounded px-2 py-1 ml-2 focus:outline-none focus:border-blue-500"
              >
                {workflows.map((w) => (
                  <option key={w.name} value={w.name}>
                    {w.name} (v{w.version})
                  </option>
                ))}
              </select>
            )}
          </div>
          <p className="text-sm text-slate-400 mt-1 max-w-2xl">
            {workflow?.description || 'Process customer orders with payment, inventory and notification steps.'}
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-3">
          {/* Auto Refresh Toggle */}
          <button
            onClick={onToggleAutoRefresh}
            title={autoRefresh ? 'Auto-refresh active (1.5s)' : 'Auto-refresh paused'}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium border transition-colors ${
              autoRefresh
                ? 'bg-slate-800/80 text-blue-400 border-blue-500/30'
                : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-slate-200'
            }`}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${autoRefresh ? 'animate-spin text-blue-400' : ''}`} style={{ animationDuration: '3s' }} />
            <span>Live Poll</span>
          </button>

          {/* Simulate Crash Button */}
          <button
            onClick={onSimulateCrash}
            disabled={!isRunning || isSimulatingCrash}
            title="Simulates an unexpected worker crash (kill -9) during execution to demonstrate automatic lease recovery"
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold border transition-all ${
              isRunning
                ? 'bg-amber-500/10 text-amber-300 border-amber-500/40 hover:bg-amber-500/20 active:scale-95'
                : 'bg-slate-900/50 text-slate-600 border-slate-800 cursor-not-allowed'
            }`}
          >
            <ZapOff className="w-3.5 h-3.5" />
            <span>Simulate Crash (kill -9)</span>
          </button>

          {/* Start Workflow CTA Button */}
          <button
            onClick={onOpenStartModal}
            className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white shadow-lg shadow-blue-600/25 transition-all duration-150 hover:shadow-blue-600/40"
          >
            <Play className="w-4 h-4 fill-white" />
            <span>Start Workflow</span>
          </button>
        </div>
      </div>
    </div>
  );
};
