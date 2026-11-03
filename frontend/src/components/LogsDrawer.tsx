import React from 'react';
import { X, Clock, Terminal, CheckCircle2, AlertTriangle, XCircle, RotateCcw } from 'lucide-react';
import { WorkflowEvent } from '../types';

interface LogsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  events: WorkflowEvent[];
  executionId: string | null;
}

export const LogsDrawer: React.FC<LogsDrawerProps> = ({
  isOpen,
  onClose,
  events,
  executionId,
}) => {
  if (!isOpen) return null;

  const getEventBadge = (eventType: string) => {
    switch (eventType) {
      case 'WORKFLOW_STARTED':
      case 'STEP_STARTED':
        return {
          bg: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
          icon: Terminal,
        };
      case 'STEP_COMPLETED':
      case 'WORKFLOW_COMPLETED':
        return {
          bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
          icon: CheckCircle2,
        };
      case 'STEP_RETRY_SCHEDULED':
        return {
          bg: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
          icon: AlertTriangle,
        };
      case 'STEP_RECOVERED':
        return {
          bg: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30',
          icon: RotateCcw,
        };
      case 'COMPENSATION_STARTED':
      case 'STEP_COMPENSATED':
        return {
          bg: 'bg-purple-500/10 text-purple-400 border-purple-500/30',
          icon: RotateCcw,
        };
      case 'STEP_FAILED':
      case 'WORKFLOW_FAILED':
        return {
          bg: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
          icon: XCircle,
        };
      default:
        return {
          bg: 'bg-slate-800 text-slate-300 border-slate-700',
          icon: Clock,
        };
    }
  };

  const formatTime = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        fractionalSecondDigits: 3,
        hour12: false
      });
    } catch {
      return iso;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-xl bg-[#0e131d] border-l border-slate-800 h-full flex flex-col shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/40">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-slate-800 text-slate-300">
              <Terminal className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-white">Execution Event Stream</h2>
              <p className="text-xs text-slate-500 font-mono">
                {executionId ? `ID: ${executionId}` : 'Audit trail'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Event List */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {events.length === 0 ? (
            <div className="text-center py-12 text-slate-500 text-sm">
              No event logs captured yet for this execution.
            </div>
          ) : (
            events.map((evt) => {
              const badge = getEventBadge(evt.eventType);
              const Icon = badge.icon;

              let parsedDetail = null;
              if (evt.detailJson) {
                try {
                  parsedDetail = JSON.parse(evt.detailJson);
                } catch {
                  parsedDetail = evt.detailJson;
                }
              }

              return (
                <div
                  key={evt.id}
                  className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:border-slate-700/80 transition-colors"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${badge.bg}`}>
                        <Icon className="w-3 h-3" />
                        {evt.eventType}
                      </span>
                      {evt.stepName && (
                        <span className="text-xs font-semibold text-slate-300">
                          {evt.stepName}
                        </span>
                      )}
                    </div>
                    <span className="text-[11px] font-mono text-slate-500">
                      {formatTime(evt.createdAt)}
                    </span>
                  </div>

                  {parsedDetail && (
                    <div className="mt-2.5 bg-black/40 rounded-lg p-2.5 border border-slate-800/60 overflow-x-auto">
                      <pre className="text-[11px] font-mono text-slate-400 whitespace-pre-wrap">
                        {typeof parsedDetail === 'string'
                          ? parsedDetail
                          : JSON.stringify(parsedDetail, null, 2)}
                      </pre>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
