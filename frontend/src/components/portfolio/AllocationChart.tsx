import React, { useState } from 'react';
import { PortfolioSummaryResponse, HoldingResponse, StockResponse } from '../../types';
import { formatCurrency } from '../../utils/formatters';
import { PieChart, Layers, Wallet, TrendingUp } from 'lucide-react';

interface AllocationChartProps {
  summary?: PortfolioSummaryResponse | null;
  holdings: HoldingResponse[];
  stocks?: StockResponse[];
}

interface SliceItem {
  id: string;
  name: string;
  category: string;
  value: number;
  percent: number;
  color: string;
  strokeColor: string;
}

const PALETTE = [
  { fill: '#10b981', stroke: '#059669', name: 'Emerald' }, // Cash / Safe
  { fill: '#3b82f6', stroke: '#2563eb', name: 'Blue' },
  { fill: '#8b5cf6', stroke: '#7c3aed', name: 'Purple' },
  { fill: '#f59e0b', stroke: '#d97706', name: 'Amber' },
  { fill: '#ec4899', stroke: '#db2777', name: 'Pink' },
  { fill: '#06b6d4', stroke: '#0891b2', name: 'Cyan' },
  { fill: '#f97316', stroke: '#ea580c', name: 'Orange' },
  { fill: '#6366f1', stroke: '#4f46e5', name: 'Indigo' },
  { fill: '#14b8a6', stroke: '#0d9488', name: 'Teal' },
  { fill: '#84cc16', stroke: '#65a30d', name: 'Lime' },
];

export const AllocationChart: React.FC<AllocationChartProps> = ({ summary, holdings, stocks }) => {
  const [viewMode, setViewMode] = useState<'holdings' | 'assets'>('holdings');

  const cash = summary ? Math.max(0, summary.cashBalance) : 100000;
  const holdingsVal = summary
    ? Math.max(0, summary.currentHoldingsValue)
    : holdings.reduce((sum, h) => sum + h.currentValue, 0);
  const totalValue = summary ? Math.max(0, summary.totalPortfolioValue) : cash + holdingsVal;

  const validTotal = totalValue > 0 ? totalValue : 1;

  // Build sector lookup map from stock catalog if available
  const sectorMap = new Map<string, string>();
  if (stocks) {
    stocks.forEach((s) => sectorMap.set(s.symbol.toUpperCase(), s.sector));
  }

  // 1. Asset Class Slices (Cash vs Total Equities)
  const assetSlices: SliceItem[] = [
    {
      id: 'cash',
      name: 'Cash Reserves',
      category: 'Liquid Cash',
      value: cash,
      percent: (cash / validTotal) * 100,
      color: PALETTE[0].fill,
      strokeColor: PALETTE[0].stroke,
    },
  ];

  if (holdingsVal > 0) {
    assetSlices.push({
      id: 'equities',
      name: 'Equities Portfolio',
      category: 'Invested Assets',
      value: holdingsVal,
      percent: (holdingsVal / validTotal) * 100,
      color: PALETTE[1].fill,
      strokeColor: PALETTE[1].stroke,
    });
  }

  // 2. Individual Holding Slices (Cash + Active Stocks)
  const holdingSlices: SliceItem[] = [
    {
      id: 'cash',
      name: 'Cash Balance',
      category: 'Liquid Cash',
      value: cash,
      percent: (cash / validTotal) * 100,
      color: PALETTE[0].fill,
      strokeColor: PALETTE[0].stroke,
    },
  ];

  holdings.forEach((h, idx) => {
    if (h.currentValue > 0) {
      const paletteIndex = ((idx + 1) % (PALETTE.length - 1)) + 1;
      const sector = sectorMap.get(h.symbol.toUpperCase()) || 'Equities';
      holdingSlices.push({
        id: h.symbol,
        name: h.symbol,
        category: sector,
        value: h.currentValue,
        percent: (h.currentValue / validTotal) * 100,
        color: PALETTE[paletteIndex].fill,
        strokeColor: PALETTE[paletteIndex].stroke,
      });
    }
  });

  const activeSlices = viewMode === 'assets' ? assetSlices : holdingSlices;

  // SVG Donut Calculations
  const radius = 60;
  const strokeWidth = 24;
  const circumference = 2 * Math.PI * radius; // ~376.99
  let accumulatedPercent = 0;

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-xl space-y-5">
      {/* Header & View Mode Switch */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800/80 pb-4">
        <div className="flex items-center gap-2">
          <div className="h-7 w-7 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <PieChart className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white tracking-tight">Asset & Portfolio Allocation</h3>
            <p className="text-[11px] text-slate-400">Capital distribution and exposure breakdown</p>
          </div>
        </div>

        <div className="flex items-center rounded-xl bg-slate-950 p-1 border border-slate-800 self-start sm:self-auto">
          <button
            onClick={() => setViewMode('holdings')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              viewMode === 'holdings'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Layers className="h-3.5 w-3.5" />
            <span>Holdings</span>
          </button>
          <button
            onClick={() => setViewMode('assets')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              viewMode === 'assets'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Wallet className="h-3.5 w-3.5" />
            <span>Asset Class</span>
          </button>
        </div>
      </div>

      {/* Main Visuals Grid */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
        {/* Donut Chart SVG */}
        <div className="md:col-span-5 flex flex-col items-center justify-center relative py-2">
          <svg
            className="w-48 h-48 transform -rotate-90"
            viewBox="0 0 160 160"
            role="img"
            aria-label="Asset allocation donut chart"
          >
            {/* Background track circle */}
            <circle
              cx="80"
              cy="80"
              r={radius}
              fill="transparent"
              stroke="#1e293b"
              strokeWidth={strokeWidth}
            />

            {/* Slices */}
            {activeSlices.map((slice) => {
              const dashLength = (slice.percent / 100) * circumference;
              const dashOffset = -(accumulatedPercent / 100) * circumference;
              accumulatedPercent += slice.percent;

              if (slice.percent <= 0) return null;

              return (
                <circle
                  key={slice.id}
                  cx="80"
                  cy="80"
                  r={radius}
                  fill="transparent"
                  stroke={slice.color}
                  strokeWidth={strokeWidth}
                  strokeDasharray={`${dashLength} ${circumference}`}
                  strokeDashoffset={dashOffset}
                  className="transition-all duration-500 ease-out hover:opacity-90"
                />
              );
            })}
          </svg>

          {/* Donut Center Content */}
          <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center">
            <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Total Value</span>
            <span className="text-base font-extrabold text-white">{formatCurrency(totalValue)}</span>
            <span className="text-[10px] text-emerald-400 font-semibold mt-0.5">
              {holdings.length > 0 ? `${holdings.length} Active Positions` : '100% Cash Liquidity'}
            </span>
          </div>
        </div>

        {/* Breakdown Legend & Progress Bars */}
        <div className="md:col-span-7 space-y-3">
          {/* Multi-Segment Proportion Bar */}
          <div className="h-2.5 w-full bg-slate-950 rounded-full overflow-hidden flex p-0.5 border border-slate-800">
            {activeSlices.map((slice) => {
              if (slice.percent <= 0) return null;
              return (
                <div
                  key={slice.id}
                  style={{ width: `${slice.percent}%`, backgroundColor: slice.color }}
                  className="h-full first:rounded-l-full last:rounded-r-full transition-all duration-300"
                  title={`${slice.name}: ${slice.percent.toFixed(2)}%`}
                />
              );
            })}
          </div>

          {/* Allocation List Items */}
          <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
            {activeSlices.map((slice) => (
              <div
                key={slice.id}
                className="flex items-center justify-between p-2 rounded-xl bg-slate-950/60 border border-slate-800/80 hover:border-slate-700 transition"
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <span
                    className="h-3 w-3 rounded-full shrink-0 shadow-sm"
                    style={{ backgroundColor: slice.color }}
                  />
                  <div className="min-w-0">
                    <div className="text-xs font-bold text-white truncate">{slice.name}</div>
                    <div className="text-[10px] text-slate-400 truncate">{slice.category}</div>
                  </div>
                </div>

                <div className="text-right shrink-0">
                  <div className="text-xs font-bold text-white">{formatCurrency(slice.value)}</div>
                  <div className="text-[11px] font-semibold text-slate-300">
                    {slice.percent.toFixed(2)}%
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Allocation Summary Footer Note */}
      {holdings.length === 0 ? (
        <div className="flex items-center gap-2 text-xs text-slate-400 bg-slate-950/40 p-3 rounded-xl border border-slate-800/60">
          <TrendingUp className="h-4 w-4 text-emerald-400 shrink-0" />
          <span>
            Your portfolio currently holds 100% cash reserves. Purchase equities from the market catalog to start diversifying your portfolio across sectors.
          </span>
        </div>
      ) : (
        <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-400 pt-1 border-t border-slate-800/60">
          <span>Cash Weight: <strong className="text-white">{((cash / validTotal) * 100).toFixed(2)}%</strong></span>
          <span>Equities Weight: <strong className="text-white">{((holdingsVal / validTotal) * 100).toFixed(2)}%</strong></span>
          <span>Risk Status: <strong className="text-emerald-400">{cash > 0 ? 'Liquid & Diversified' : 'Fully Invested'}</strong></span>
        </div>
      )}
    </div>
  );
};