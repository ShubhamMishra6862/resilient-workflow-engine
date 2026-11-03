import React from 'react';
import { ShoppingCart, CreditCard, Package, Mail, ArrowRight, CheckCircle2, RotateCw, AlertTriangle, XCircle, RotateCcw } from 'lucide-react';
import { StepExecution, StepStatus } from '../types';

interface PipelineViewProps {
  steps: StepExecution[];
  currentStepIndex: number;
}

export const PipelineView: React.FC<PipelineViewProps> = ({ steps }) => {
  // Map step name to icon
  const getStepIcon = (name: string) => {
    const lower = name.toLowerCase();
    if (lower.includes('order') || lower.includes('cart')) return ShoppingCart;
    if (lower.includes('pay') || lower.includes('credit')) return CreditCard;
    if (lower.includes('inventory') || lower.includes('stock')) return Package;
    if (lower.includes('notification') || lower.includes('mail')) return Mail;
    return Package;
  };

  const formatStepName = (name: string) => {
    // Convert CamelCase to readable Title Case (e.g. ValidateOrder -> Validate Order)
    return name.replace(/([A-Z])/g, ' $1').trim();
  };

  const getStatusStyling = (status: StepStatus) => {
    switch (status) {
      case 'SUCCESS':
        return {
          border: 'border-emerald-500/50 hover:border-emerald-500',
          bg: 'bg-emerald-950/20',
          iconBg: 'bg-emerald-500/10 text-emerald-400',
          badgeText: 'text-emerald-400',
          label: 'Completed',
          indicator: <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />,
        };
      case 'RUNNING':
        return {
          border: 'border-blue-500/60 shadow-lg shadow-blue-500/10 hover:border-blue-400',
          bg: 'bg-blue-950/20',
          iconBg: 'bg-blue-500/10 text-blue-400',
          badgeText: 'text-blue-400',
          label: 'Running',
          indicator: <RotateCw className="w-3.5 h-3.5 text-blue-400 animate-spin" />,
        };
      case 'RETRY_SCHEDULED':
        return {
          border: 'border-amber-500/50 hover:border-amber-500',
          bg: 'bg-amber-950/20',
          iconBg: 'bg-amber-500/10 text-amber-400',
          badgeText: 'text-amber-400',
          label: 'Retrying',
          indicator: <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />,
        };
      case 'FAILED':
        return {
          border: 'border-rose-500/50 hover:border-rose-500',
          bg: 'bg-rose-950/20',
          iconBg: 'bg-rose-500/10 text-rose-400',
          badgeText: 'text-rose-400',
          label: 'Failed',
          indicator: <XCircle className="w-3.5 h-3.5 text-rose-400" />,
        };
      case 'COMPENSATED':
        return {
          border: 'border-purple-500/50 hover:border-purple-500',
          bg: 'bg-purple-950/20',
          iconBg: 'bg-purple-500/10 text-purple-400',
          badgeText: 'text-purple-400',
          label: 'Compensated',
          indicator: <RotateCcw className="w-3.5 h-3.5 text-purple-400" />,
        };
      case 'PENDING':
      default:
        return {
          border: 'border-slate-800 hover:border-slate-700',
          bg: 'bg-slate-900/40',
          iconBg: 'bg-slate-800/60 text-slate-500',
          badgeText: 'text-slate-400',
          label: 'Pending',
          indicator: <span className="w-2 h-2 rounded-full bg-slate-600" />,
        };
    }
  };

  if (!steps || steps.length === 0) {
    return (
      <div className="bg-[#101520] border border-slate-800/80 rounded-xl p-8 text-center text-slate-500">
        No active execution steps to visualize. Click "Start Workflow" to run a new instance.
      </div>
    );
  }

  return (
    <div className="mb-8">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-center">
        {steps.map((step, index) => {
          const Icon = getStepIcon(step.stepName);
          const styling = getStatusStyling(step.status);
          const isLast = index === steps.length - 1;

          return (
            <React.Fragment key={step.id || step.stepName}>
              {/* Step Card */}
              <div
                className={`relative flex flex-col justify-between p-4 rounded-xl border ${styling.border} ${styling.bg} transition-all duration-200 group`}
              >
                {/* Step Top Row: Icon + Label */}
                <div className="flex items-start justify-between mb-3">
                  <div className={`p-2.5 rounded-lg ${styling.iconBg}`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  {step.attemptCount > 1 && (
                    <span className="text-[11px] font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                      Attempt {step.attemptCount}
                    </span>
                  )}
                </div>

                {/* Step Details */}
                <div>
                  <h3 className="text-sm font-semibold text-slate-100 group-hover:text-white transition-colors">
                    {formatStepName(step.stepName)}
                  </h3>
                  <div className="flex items-center gap-1.5 mt-1.5">
                    {styling.indicator}
                    <span className={`text-xs font-medium ${styling.badgeText}`}>
                      {styling.label}
                    </span>
                  </div>
                </div>

                {/* Duration / Retrying indicator at bottom */}
                {step.durationMs && step.status === 'SUCCESS' && (
                  <div className="mt-2 pt-2 border-t border-slate-800/40 text-[11px] text-slate-400 font-mono">
                    Duration: {(step.durationMs / 1000).toFixed(1)}s
                  </div>
                )}
                {step.status === 'RETRY_SCHEDULED' && (
                  <div className="mt-2 pt-2 border-t border-amber-500/20 text-[11px] text-amber-300/80">
                    Backoff retry scheduled...
                  </div>
                )}
              </div>

              {/* Connecting Arrow for larger screens (rendered between items if needed) */}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
};
