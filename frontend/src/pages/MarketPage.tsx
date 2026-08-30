import React, { useState } from 'react';
import { useMarketData } from '../hooks/useMarketData';
import { usePortfolio } from '../hooks/usePortfolio';
import { useAiAdvisor } from '../hooks/useAiAdvisor';
import { StockTable } from '../components/market/StockTable';
import { TradeModal } from '../components/trading/TradeModal';
import { StockAnalysisModal } from '../components/ai/StockAnalysisModal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { StockResponse, StockAnalysisResponse } from '../types';
import { Store, RefreshCw } from 'lucide-react';

export const MarketPage: React.FC = () => {
  const { stocks, isLoading, error, refresh } = useMarketData();
  const { summary, holdings, refresh: refreshPort } = usePortfolio();
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
      // Error handled in hook
    }
  };

  const userHolding = holdings.find((h) => h.symbol === selectedStock?.symbol);

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <Store className="h-6 w-6 text-emerald-400" />
            <h1 className="text-2xl font-bold text-white tracking-tight">Market Equities Catalog</h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Simulated Indian market equities catalog for trading and AI fundamental analysis
          </p>
        </div>
        <button
          onClick={refresh}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-800 bg-slate-900 text-xs font-semibold text-slate-300 hover:text-white hover:bg-slate-800 transition"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          <span>Refresh Feed</span>
        </button>
      </div>

      {error && <ErrorAlert message={error} />}

      {isLoading && stocks.length === 0 ? (
        <LoadingSpinner size="lg" message="Loading market catalog..." />
      ) : (
        <StockTable stocks={stocks} onTrade={handleOpenTrade} onAnalyze={handleAnalyze} />
      )}

      {/* Trade Modal */}
      <TradeModal
        isOpen={isTradeModalOpen}
        stock={selectedStock}
        initialType={tradeType}
        maxSellQuantity={userHolding?.quantity || 0}
        availableCash={summary?.cashBalance || 0}
        onClose={() => setIsTradeModalOpen(false)}
        onTradeComplete={() => {
          refreshPort();
          refresh();
        }}
      />

      {/* AI Analysis Modal */}
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