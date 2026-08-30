import React, { useState } from 'react';
import { usePortfolio } from '../hooks/usePortfolio';
import { useMarketData } from '../hooks/useMarketData';
import { MetricCard } from '../components/common/MetricCard';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { HoldingsTable } from '../components/portfolio/HoldingsTable';
import { StockTable } from '../components/market/StockTable';
import { TradeModal } from '../components/trading/TradeModal';
import { StockAnalysisModal } from '../components/ai/StockAnalysisModal';
import { useAiAdvisor } from '../hooks/useAiAdvisor';
import { StockResponse, StockAnalysisResponse } from '../types';
import { formatCurrency } from '../utils/formatters';
import { Wallet, Briefcase, TrendingUp, DollarSign, Activity, Sparkles, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const { summary, holdings, isLoading: isPortLoading, error: portError, refresh: refreshPort } = usePortfolio();
  const { stocks, error: marketError, refresh: refreshMarket } = useMarketData();
  const { analyzeStock, isAiLoading } = useAiAdvisor();

  const [selectedStock, setSelectedStock] = useState<StockResponse | null>(null);
  const [tradeType, setTradeType] = useState<'BUY' | 'SELL'>('BUY');
  const [isTradeModalOpen, setIsTradeModalOpen] = useState(false);

  const [analyzedSymbol, setAnalyzedSymbol] = useState('');
  const [analysisData, setAnalysisData] = useState<StockAnalysisResponse | null>(null);
  const [isAnalysisModalOpen, setIsAnalysisModalOpen] = useState(false);

  const handleOpenTrade = (stock: StockResponse, type: 'BUY' | 'SELL') => {
    setSelectedStock(stock);
    setTradeType(type);
    setIsTradeModalOpen(true);
  };

  const handleAnalyze = async (stock: StockResponse) => {
    setAnalyzedSymbol(stock.symbol);
    setIsAnalysisModalOpen(true);
    try {
      const data = await analyzeStock(stock.symbol);
      setAnalysisData(data);
    } catch {
      // Error in hook
    }
  };

  const handleRefreshAll = () => {
    refreshPort();
    refreshMarket();
  };

  if (isPortLoading && !summary) {
    return <LoadingSpinner size="lg" message="Loading portfolio intelligence..." />;
  }

  const userHoldingOfSelected = holdings.find((h) => h.symbol === selectedStock?.symbol);

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Portfolio Overview</h1>
          <p className="text-xs text-slate-400 mt-1">Real-time valuation, active equity positions, and AI advisor insights</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleRefreshAll}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-800 bg-slate-900 text-xs font-semibold text-slate-300 hover:text-white hover:bg-slate-800 transition"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            <span>Refresh</span>
          </button>
          <Link
            to="/ai"
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-emerald-600 font-bold text-white hover:bg-emerald-500 transition text-xs shadow-lg"
          >
            <Sparkles className="h-3.5 w-3.5" />
            <span>AI Advisor</span>
          </Link>
        </div>
      </div>

      {portError && <ErrorAlert message={portError} />}
      {marketError && <ErrorAlert message={marketError} />}

      {/* Metrics Row */}
      {summary && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricCard
            label="Total Portfolio Value"
            value={formatCurrency(summary.totalPortfolioValue)}
            change={summary.portfolioReturnPercent}
            changeLabel="overall"
            icon={TrendingUp}
            subtext="Initial: ₹100,000.00"
            highlight
          />
          <MetricCard
            label="Cash Balance"
            value={formatCurrency(summary.cashBalance)}
            icon={Wallet}
            subtext="Available uninvested capital"
          />
          <MetricCard
            label="Current Holdings Value"
            value={formatCurrency(summary.currentHoldingsValue)}
            icon={Briefcase}
            subtext={`Cost Basis: ${formatCurrency(summary.totalInvested)}`}
          />
          <MetricCard
            label="Unrealized P&L"
            value={formatCurrency(summary.unrealizedPnL)}
            change={summary.unrealizedPnLPercent}
            changeLabel="return"
            icon={Activity}
            subtext="On current active positions"
          />
        </div>
      )}

      {/* Holdings Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Briefcase className="h-5 w-5 text-emerald-400" />
            <h2 className="text-base font-bold text-white">Active Positions</h2>
          </div>
          <Link to="/portfolio" className="text-xs text-emerald-400 font-semibold hover:underline">
            View Details →
          </Link>
        </div>
        <HoldingsTable holdings={holdings} onTradeStock={handleOpenTrade} />
      </div>

      {/* Market Snapshot */}
      <div className="space-y-4 pt-4 border-t border-slate-800/80">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <DollarSign className="h-5 w-5 text-emerald-400" />
            <h2 className="text-base font-bold text-white">Equities Catalog</h2>
          </div>
          <Link to="/market" className="text-xs text-emerald-400 font-semibold hover:underline">
            View All Equities →
          </Link>
        </div>
        <StockTable
          stocks={stocks.slice(0, 5)}
          onTrade={handleOpenTrade}
          onAnalyze={handleAnalyze}
        />
      </div>

      {/* Modals */}
      <TradeModal
        isOpen={isTradeModalOpen}
        stock={selectedStock}
        initialType={tradeType}
        maxSellQuantity={userHoldingOfSelected?.quantity || 0}
        availableCash={summary?.cashBalance || 0}
        onClose={() => setIsTradeModalOpen(false)}
        onTradeComplete={() => {
          refreshPort();
          refreshMarket();
        }}
      />

      <StockAnalysisModal
        isOpen={isAnalysisModalOpen}
        symbol={analyzedSymbol}
        analysisData={analysisData}
        isLoading={isAiLoading}
        onClose={() => {
          setIsAnalysisModalOpen(false);
          setAnalysisData(null);
        }}
      />
    </div>
  );
};