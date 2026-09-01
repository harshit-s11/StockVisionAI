import React, { useState } from 'react';
import { usePortfolio } from '../hooks/usePortfolio';
import { useMarketData } from '../hooks/useMarketData';
import { MetricCard } from '../components/common/MetricCard';
import { HoldingsTable } from '../components/portfolio/HoldingsTable';
import { AllocationChart } from '../components/portfolio/AllocationChart';
import { TradeModal } from '../components/trading/TradeModal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { StockResponse } from '../types';
import { formatCurrency } from '../utils/formatters';
import { Briefcase, Wallet, TrendingUp, Activity, RefreshCw } from 'lucide-react';

export const PortfolioPage: React.FC = () => {
  const { summary, holdings, isLoading, error, refresh } = usePortfolio();
  const { stocks } = useMarketData();
  const [selectedStock, setSelectedStock] = useState<StockResponse | null>(null);
  const [tradeType, setTradeType] = useState<'BUY' | 'SELL'>('BUY');
  const [isTradeModalOpen, setIsTradeModalOpen] = useState(false);

  const handleOpenTrade = (stock: StockResponse, type: 'BUY' | 'SELL') => {
    setSelectedStock(stock);
    setTradeType(type);
    setIsTradeModalOpen(true);
  };

  const userHolding = holdings.find((h) => h.symbol === selectedStock?.symbol);

  if (isLoading && !summary) {
    return <LoadingSpinner size="lg" message="Loading portfolio valuation..." />;
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <Briefcase className="h-6 w-6 text-emerald-400" />
            <h1 className="text-2xl font-bold text-white tracking-tight">Portfolio & Holdings</h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Authoritative server-side cost basis, unrealized P&L, and asset allocation breakdown
          </p>
        </div>
        <button
          onClick={refresh}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-800 bg-slate-900 text-xs font-semibold text-slate-300 hover:text-white hover:bg-slate-800 transition"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          <span>Refresh Portfolio</span>
        </button>
      </div>

      {error && <ErrorAlert message={error} />}

      {summary && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricCard
            label="Total Portfolio Value"
            value={formatCurrency(summary.totalPortfolioValue)}
            change={summary.portfolioReturnPercent}
            changeLabel="overall"
            icon={TrendingUp}
            subtext="Cash + Holdings value"
            highlight
          />
          <MetricCard
            label="Available Cash"
            value={formatCurrency(summary.cashBalance)}
            icon={Wallet}
            subtext="Instant liquidity"
          />
          <MetricCard
            label="Cost Basis (Total Invested)"
            value={formatCurrency(summary.totalInvested)}
            icon={Briefcase}
            subtext="Weighted average purchase cost"
          />
          <MetricCard
            label="Unrealized P&L"
            value={formatCurrency(summary.unrealizedPnL)}
            change={summary.unrealizedPnLPercent}
            changeLabel="return"
            icon={Activity}
            subtext="Open positions profit / loss"
          />
        </div>
      )}

      {/* Asset Allocation & Sector Breakdown Chart */}
      <AllocationChart summary={summary} holdings={holdings} stocks={stocks} />

      {/* Active Positions */}
      <div className="space-y-4">
        <h2 className="text-base font-bold text-white">Active Positions</h2>
        <HoldingsTable holdings={holdings} onTradeStock={handleOpenTrade} />
      </div>

      <TradeModal
        isOpen={isTradeModalOpen}
        stock={selectedStock}
        initialType={tradeType}
        maxSellQuantity={userHolding?.quantity || 0}
        availableCash={summary?.cashBalance || 0}
        onClose={() => setIsTradeModalOpen(false)}
        onTradeComplete={refresh}
      />
    </div>
  );
};