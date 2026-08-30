import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Store, Briefcase, History, Sparkles } from 'lucide-react';

export const Sidebar: React.FC = () => {
  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/market', label: 'Market Catalog', icon: Store },
    { to: '/portfolio', label: 'Portfolio & Holdings', icon: Briefcase },
    { to: '/transactions', label: 'Transaction Logs', icon: History },
    { to: '/ai', label: 'AI Advisor', icon: Sparkles },
  ];

  return (
    <aside className="w-64 border-r border-slate-800 bg-slate-950 flex flex-col justify-between p-4 hidden md:flex shrink-0">
      <nav className="space-y-1.5">
        <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
          Navigation
        </div>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                isActive
                  ? 'bg-slate-900 text-white border border-slate-700/80 shadow-sm font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/50'
              }`
            }
          >
            <item.icon className="h-4 w-4 shrink-0 text-slate-400" />
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="rounded-xl border border-slate-800/80 bg-slate-900/50 p-3.5 text-xs text-slate-400 space-y-1">
        <div className="font-semibold text-slate-300 flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
          Educational Platform
        </div>
        <p className="text-slate-400 leading-relaxed">
          Simulated trading with ₹100,000 demo capital and Gemini-powered RAG intelligence.
        </p>
      </div>
    </aside>
  );
};