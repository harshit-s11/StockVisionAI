import React from 'react';

export const Badge: React.FC<{
  children: React.ReactNode;
  variant?: 'demo' | 'buy' | 'sell' | 'neutral' | 'success' | 'danger';
}> = ({ children, variant = 'neutral' }) => {
  const styles = {
    demo: 'bg-amber-950/60 text-amber-300 border-amber-800/50',
    buy: 'bg-emerald-950/60 text-emerald-300 border-emerald-800/50',
    sell: 'bg-rose-950/60 text-rose-300 border-rose-800/50',
    success: 'bg-emerald-950/60 text-emerald-400 border-emerald-800/40',
    danger: 'bg-rose-950/60 text-rose-400 border-rose-800/40',
    neutral: 'bg-slate-800 text-slate-300 border-slate-700',
  };

  return (
    <span
      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold tracking-wide border uppercase ${styles[variant]}`}
    >
      {children}
    </span>
  );
};