import React from 'react';
import { FileText, RefreshCw, AlertCircle } from 'lucide-react';
import { StepExecution, StepStatus } from '../types';

interface ExecutionHistoryTableProps {
  steps: StepExecution[];
  onOpenLogs: () => void;
  onRetryStep: (stepName: string) => void;
}

export const ExecutionHistoryTable: React.FC<ExecutionHistoryTableProps> = ({
  steps,
  onOpenLogs,
  onRetryStep,
}) => {
  const formatStepName = (name: string) => {
    return name.replace(/([A-Z])/g, ' $1').trim();
  };

  const formatTimestamp = (isoDate: string | null) => {
    if (!isoDate) return '-';
    try {
      const d = new Date(isoDate);
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      }) + ' ' + d.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    } catch {
      return isoDate;
    }
  };

  const formatDuration = (ms: number | null, status: StepStatus) => {
    if (status === 'RUNNING') return 'Running...';
    if (!ms) return '-';
    return (ms / 1000).toFixed(1) + 's';
  };

  const renderStatusBadge = (status: StepStatus) => {
    switch (status) {
      case 'SUCCESS':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-400">
            <span className="w-2 h-2 rounded-full bg-emerald-400" />
            Success
          </span>
        );
      case 'RUNNING':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-blue-400">
            <span className="w-2 h-2 rounded-full bg-blue-400 animate-ping" />
            Running
          </span>
        );
      case 'RETRY_SCHEDULED':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-amber-400">
            <span className="w-2 h-2 rounded-full bg-amber-400" />
            Retrying
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-rose-400">
            <span className="w-2 h-2 rounded-full bg-rose-400" />
            Failed
          </span>
        );
      case 'COMPENSATED':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-purple-400">
            <span className="w-2 h-2 rounded-full bg-purple-400" />
            Compensated
          </span>
        );
      case 'PENDING':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-500">
            <span className="w-2 h-2 rounded-full bg-slate-600" />
            Pending
          </span>
        );
    }
  };

  return (
    <div className="bg-[#101520] border border-slate-800/80 rounded-xl overflow-hidden shadow-xl">
      {/* Table Header Row */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800/80">
        <div>
          <h2 className="text-base font-semibold text-white tracking-tight">Execution History</h2>
          <p className="text-xs text-slate-500 mt-0.5">Durable step execution progress and attempt telemetry</p>
        </div>
        <button
          onClick={onOpenLogs}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-300 hover:text-white bg-slate-800/60 hover:bg-slate-800 border border-slate-700/60 transition-colors"
        >
          <FileText className="w-3.5 h-3.5 text-slate-400" />
          <span>View Logs</span>
        </button>
      </div>

      {/* Table Content */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-800/60 text-xs font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/40">
              <th className="px-6 py-3.5">Step</th>
              <th className="px-6 py-3.5">Status</th>
              <th className="px-6 py-3.5">Started At</th>
              <th className="px-6 py-3.5">Duration</th>
              <th className="px-6 py-3.5">Attempts</th>
              <th className="px-6 py-3.5 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/50 font-normal text-slate-300">
            {steps.map((step) => (
              <tr key={step.id || step.stepName} className="hover:bg-slate-850/40 transition-colors">
                <td className="px-6 py-4 font-medium text-white flex items-center gap-2">
                  <span>{formatStepName(step.stepName)}</span>
                  {step.error && (
                    <span title={step.error} className="cursor-help">
                      <AlertCircle className="w-4 h-4 text-amber-400/80" />
                    </span>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  {renderStatusBadge(step.status)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-xs text-slate-400 font-mono">
                  {formatTimestamp(step.startedAt)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-xs text-slate-400 font-mono">
                  {formatDuration(step.durationMs, step.status)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-xs text-slate-400 font-mono">
                  {step.attemptCount > 0 ? step.attemptCount : '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right">
                  {step.status === 'FAILED' && (
                    <button
                      onClick={() => onRetryStep(step.stepName)}
                      className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-blue-400 hover:text-blue-300 bg-blue-500/10 hover:bg-blue-500/20 rounded border border-blue-500/30 transition-all"
                    >
                      <RefreshCw className="w-3 h-3" />
                      Retry
                    </button>
                  )}
                  {step.status !== 'FAILED' && (
                    <span className="text-xs text-slate-600">-</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
