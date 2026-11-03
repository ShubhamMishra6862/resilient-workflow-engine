import React, { useState } from 'react';
import { X, Play, ShieldAlert, Sparkles } from 'lucide-react';
import { StartWorkflowParams } from '../types';

interface StartWorkflowModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (params: StartWorkflowParams) => void;
  workflowName: string;
}

export const StartWorkflowModal: React.FC<StartWorkflowModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  workflowName,
}) => {
  const [orderId, setOrderId] = useState(`ORD-${Math.floor(10000 + Math.random() * 90000)}`);
  const [customerEmail, setCustomerEmail] = useState('alex.chen@example.com');
  const [amount, setAmount] = useState(149.99);
  const [simulateFailure, setSimulateFailure] = useState(true);
  const [failAttempts, setFailAttempts] = useState(1);
  const [failPermanently, setFailPermanently] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      orderId,
      customerEmail,
      amount: Number(amount),
      simulateFailure,
      failAttempts: Number(failAttempts),
      failPermanently,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="w-full max-w-lg bg-[#111723] border border-slate-800 rounded-2xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/40">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400">
              <Play className="w-4 h-4 fill-blue-400" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-white">Start New Workflow</h2>
              <p className="text-xs text-slate-400 font-mono capitalize">{workflowName}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">Order ID</label>
              <input
                type="text"
                value={orderId}
                onChange={(e) => setOrderId(e.target.value)}
                required
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-slate-200 focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">Amount ($)</label>
              <input
                type="number"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                required
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-slate-200 focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1.5">Customer Email</label>
            <input
              type="email"
              value={customerEmail}
              onChange={(e) => setCustomerEmail(e.target.value)}
              required
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-200 focus:outline-none focus:border-blue-500"
            />
          </div>

          {/* Crash-Resilience Simulation Controls */}
          <div className="pt-3 border-t border-slate-800/80">
            <div className="flex items-center gap-2 mb-2 text-xs font-semibold text-amber-400">
              <ShieldAlert className="w-4 h-4 text-amber-400" />
              <span>Fault Injection & Resilience Playground</span>
            </div>

            <div className="space-y-3 bg-slate-950/60 p-3.5 rounded-xl border border-amber-500/20">
              {/* Failure Simulation Toggle */}
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={simulateFailure}
                  disabled={failPermanently}
                  onChange={(e) => setSimulateFailure(e.target.checked)}
                  className="mt-0.5 rounded border-slate-700 text-blue-600 focus:ring-0 bg-slate-900"
                />
                <div>
                  <span className="text-xs font-medium text-slate-200">
                    Simulate transient failure on Update Inventory
                  </span>
                  <p className="text-[11px] text-slate-500 mt-0.5 leading-tight">
                    Tests exponential backoff with jitter and DB-driven retry scheduling without sleeping threads.
                  </p>
                </div>
              </label>

              {simulateFailure && !failPermanently && (
                <div className="pl-6 flex items-center gap-3">
                  <label className="text-xs text-slate-400">Fail attempts:</label>
                  <select
                    value={failAttempts}
                    onChange={(e) => setFailAttempts(Number(e.target.value))}
                    className="bg-slate-900 border border-slate-700 text-xs text-slate-200 rounded px-2 py-1 focus:outline-none"
                  >
                    <option value={1}>1 time (succeeds on attempt 2 - matches screenshot!)</option>
                    <option value={2}>2 times (succeeds on attempt 3)</option>
                  </select>
                </div>
              )}

              {/* SAGA Permanent Failure Toggle */}
              <label className="flex items-start gap-2.5 cursor-pointer pt-2 border-t border-slate-800/60">
                <input
                  type="checkbox"
                  checked={failPermanently}
                  onChange={(e) => setFailPermanently(e.target.checked)}
                  className="mt-0.5 rounded border-slate-700 text-rose-600 focus:ring-0 bg-slate-900"
                />
                <div>
                  <span className="text-xs font-medium text-rose-300">
                    Trigger SAGA backward compensation
                  </span>
                  <p className="text-[11px] text-slate-500 mt-0.5 leading-tight">
                    Simulates unrecoverable inventory depletion, automatically rolling back and refunding payment.
                  </p>
                </div>
              </label>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800/80">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-500 active:bg-blue-700 rounded-lg shadow-md shadow-blue-600/30 transition-all"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>Launch Execution</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
