import React, { useState, useMemo } from 'react';
import { StockResponse } from '../../types';
import { formatCurrency, formatPercent } from '../../utils/formatters';
import { Badge } from '../common/Badge';
import { Sparkles, ShoppingCart, Search, X, Filter } from 'lucide-react';

interface StockTableProps {
  stocks: StockResponse[];
  onTrade?: (stock: StockResponse, type: 'BUY' | 'SELL') => void;
  onAnalyze?: (stock: StockResponse) => void;
}

export const StockTable: React.FC<StockTableProps> = ({ stocks, onTrade, onAnalyze }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSector, setSelectedSector] = useState('All');

  // Dynamically derive unique sectors present in the current stocks catalog
  const availableSectors = useMemo(() => {
    const sectors = Array.from(new Set(stocks.map((s) => s.sector).filter(Boolean)));
    return ['All', ...sectors];
  }, [stocks]);

  // Combined search and sector filtering (AND logic)
  const filteredStocks = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    return stocks.filter((stock) => {
      const matchesSearch =
        !term ||
        stock.symbol.toLowerCase().includes(term) ||
        stock.companyName.toLowerCase().includes(term) ||
        stock.sector.toLowerCase().includes(term);

      const matchesSector = selectedSector === 'All' || stock.sector === selectedSector;

      return matchesSearch && matchesSector;
    });
  }, [stocks, searchTerm, selectedSector]);

  const handleResetFilters = () => {
    setSearchTerm('');
    setSelectedSector('All');
  };

  return (
    <div className="space-y-4">
      {/* Controls: Search Input & Metadata */}
      <div className="flex flex-col sm:flex-row gap-3 justify-between items-stretch sm:items-center">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search equities by symbol, company, or sector..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-9 py-2 rounded-xl border border-slate-800 bg-slate-900 text-sm text-white placeholder-slate-500 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3 top-2.5 p-0.5 rounded-md text-slate-400 hover:text-white hover:bg-slate-800 transition"
              aria-label="Clear search"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-2 text-xs text-slate-400 self-end sm:self-auto">
          <span>
            Showing <strong>{filteredStocks.length}</strong> of {stocks.length} equities
          </span>
          <Badge variant="demo">Demo Catalog</Badge>
        </div>
      </div>

      {/* Dynamic Sector Filter Buttons */}
      {availableSectors.length > 1 && (
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-0.5 scrollbar-thin">
          <div className="flex items-center gap-1 text-xs text-slate-500 font-semibold mr-1 shrink-0">
            <Filter className="h-3 w-3" />
            <span>Sector:</span>
          </div>
          {availableSectors.map((sector) => {
            const isActive = selectedSector === sector;
            return (
              <button
                key={sector}
                onClick={() => setSelectedSector(sector)}
                className={`px-3 py-1 rounded-lg text-xs font-semibold whitespace-nowrap transition border ${
                  isActive
                    ? 'bg-emerald-600 text-white border-emerald-500 shadow-sm'
                    : 'bg-slate-900 text-slate-400 border-slate-800 hover:bg-slate-800 hover:text-slate-200'
                }`}
              >
                {sector}
              </button>
            );
          })}
        </div>
      )}

      {/* Table Container or Empty Filtered State */}
      {filteredStocks.length === 0 ? (
        <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-10 text-center space-y-3">
          <div className="text-slate-400 font-semibold text-sm">No equities match your search or filter criteria</div>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            {searchTerm && selectedSector !== 'All'
              ? `No stocks found in "${selectedSector}" matching "${searchTerm}".`
              : searchTerm
              ? `No stocks found matching "${searchTerm}".`
              : `No stocks found in sector "${selectedSector}".`}
          </p>
          <button
            onClick={handleResetFilters}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 text-emerald-400 border border-slate-700 hover:bg-slate-700 transition"
          >
            <X className="h-3 w-3" />
            <span>Reset Filters</span>
          </button>
        </div>
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="border-b border-slate-800 bg-slate-950/60 text-xs uppercase text-slate-400 font-semibold tracking-wider">
                <tr>
                  <th className="px-5 py-3.5">Symbol / Company</th>
                  <th className="px-5 py-3.5">Sector</th>
                  <th className="px-5 py-3.5 text-right">Price</th>
                  <th className="px-5 py-3.5 text-right">Prev Close</th>
                  <th className="px-5 py-3.5 text-right">24h Change</th>
                  <th className="px-5 py-3.5 text-center">Data Feed</th>
                  <th className="px-5 py-3.5 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {filteredStocks.map((stock) => {
                  const isPositive = stock.change >= 0;
                  return (
                    <tr key={stock.symbol} className="hover:bg-slate-850/50 transition">
                      <td className="px-5 py-4">
                        <div className="font-bold text-white">{stock.symbol}</div>
                        <div className="text-xs text-slate-400 truncate max-w-xs">{stock.companyName}</div>
                      </td>
                      <td className="px-5 py-4">
                        <span className="text-xs font-medium px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                          {stock.sector}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-right font-bold text-white">
                        {formatCurrency(stock.currentPrice)}
                      </td>
                      <td className="px-5 py-4 text-right text-slate-400">
                        {formatCurrency(stock.previousClose)}
                      </td>
                      <td className="px-5 py-4 text-right font-bold">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded text-xs ${
                            isPositive
                              ? 'text-emerald-400 bg-emerald-950/60 border border-emerald-800/40'
                              : 'text-rose-400 bg-rose-950/60 border border-rose-800/40'
                          }`}
                        >
                          {formatCurrency(stock.change)} ({formatPercent(stock.changePercent)})
                        </span>
                      </td>
                      <td className="px-5 py-4 text-center">
                        <Badge variant="demo">{stock.dataStatus}</Badge>
                      </td>
                      <td className="px-5 py-4 text-center">
                        <div className="flex items-center justify-center gap-2">
                          {onAnalyze && (
                            <button
                              onClick={() => onAnalyze(stock)}
                              className="flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/30 hover:bg-emerald-500/20 transition"
                              title="Analyze with AI Advisor"
                            >
                              <Sparkles className="h-3 w-3" />
                              <span>Analyze</span>
                            </button>
                          )}
                          {onTrade && (
                            <button
                              onClick={() => onTrade(stock, 'BUY')}
                              className="flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 text-slate-200 border border-slate-700 hover:bg-slate-700 transition"
                              title="Place Trade Order"
                            >
                              <ShoppingCart className="h-3 w-3" />
                              <span>Trade</span>
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};