import React from 'react';
import { GitBranch, ListChecks, CheckSquare, Calendar, Settings } from 'lucide-react';

interface SidebarProps {
  currentTab: string;
  onSelectTab: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, onSelectTab }) => {
  const navItems = [
    { id: 'workflows', label: 'Workflows', icon: GitBranch },
    { id: 'executions', label: 'Executions', icon: ListChecks },
    { id: 'tasks', label: 'Tasks', icon: CheckSquare },
    { id: 'schedules', label: 'Schedules', icon: Calendar },
    { id: 'settings', label: 'Settings', icon: Settings },
  ];

  return (
    <aside className="w-64 bg-[#090d15] border-r border-slate-800/80 flex flex-col h-screen select-none">
      {/* Brand Header */}
      <div className="h-16 flex items-center px-6 gap-3 border-b border-slate-800/60">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-indigo-600 via-indigo-500 to-purple-500 flex items-center justify-center shadow-md shadow-indigo-500/20">
          <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="12 2 2 8.5 12 15 22 8.5 12 2" />
            <polygon points="2 15.5 12 22 22 15.5 12 9 2 15.5" />
          </svg>
        </div>
        <span className="text-lg font-bold tracking-tight text-white">FlowLab</span>
      </div>

      {/* Navigation List */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                isActive
                  ? 'bg-blue-600/15 text-blue-400 border border-blue-500/30 shadow-sm shadow-blue-500/10'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-blue-400' : 'text-slate-400'}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Engine Status Footer */}
      <div className="p-4 m-3 rounded-xl bg-slate-900/60 border border-slate-800/60 text-xs">
        <div className="flex items-center justify-between mb-1.5">
          <span className="text-slate-400 font-medium">Engine Mode</span>
          <span className="flex items-center gap-1.5 text-emerald-400 font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            Crash-Proof
          </span>
        </div>
        <p className="text-[11px] text-slate-500 leading-relaxed">
          State durably saved in Postgres before/after every step transition.
        </p>
      </div>
    </aside>
  );
};
