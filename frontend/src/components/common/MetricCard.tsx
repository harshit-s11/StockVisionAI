import React from 'react';
import { LucideIcon } from 'lucide-react';
import { formatPercent } from '../../utils/formatters';

interface MetricCardProps {
  label: string;
  value: string;
  change?: number;
  changeLabel?: string;
  icon?: LucideIcon;
  subtext?: string;
  highlight?: boolean;
}

export const MetricCard: React.FC<MetricCardProps> = ({
  label,
  value,
  change,
  changeLabel,
  icon: Icon,
  subtext,
  highlight = false,
}) => {
  const isPositive = change !== undefined && change > 0;
  const isNegative = change !== undefined && change < 0;

  return (
    <div
      className={`rounded-xl border p-5 transition-all ${
        highlight
          ? 'bg-slate-900/90 border-brand-500/30 shadow-lg shadow-brand-500/5'
          : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
      }`}
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
          {label}
        </span>
        {Icon && <Icon className="h-4 w-4 text-slate-500" />}
      </div>
      <div className="mt-3 flex items-baseline gap-2">
        <span className="text-2xl font-bold tracking-tight text-white">{value}</span>
        {change !== undefined && (
          <span
            className={`inline-flex items-center text-xs font-medium px-2 py-0.5 rounded ${
              isPositive
                ? 'text-emerald-400 bg-emerald-950/60 border border-emerald-800/40'
                : isNegative
                ? 'text-rose-400 bg-rose-950/60 border border-rose-800/40'
                : 'text-slate-400 bg-slate-800/60'
            }`}
          >
            {formatPercent(change)} {changeLabel}
          </span>
        )}
      </div>
      {subtext && <p className="mt-1 text-xs text-slate-400">{subtext}</p>}
    </div>
  );
};