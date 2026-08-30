import React from 'react';
import { HoldingResponse, StockResponse } from '../../types';
import { formatCurrency, formatPercent } from '../../utils/formatters';

interface HoldingsTableProps {
  holdings: HoldingResponse[];
  onTradeStock?: (stock: StockResponse, type: 'BUY' | 'SELL') => void;
}

export const HoldingsTable: React.FC<HoldingsTableProps> = ({ holdings, onTradeStock }) => {
  if (holdings.length === 0) {
    return (
      <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-12 text-center space-y-2">
        <div className="text-slate-400 font-semibold text-base">No active holdings in your portfolio</div>
        <p className="text-xs text-slate-400 max-w-sm mx-auto">
          Explore the Market Catalog to buy equities using your ₹100,000 demo capital.
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="border-b border-slate-800 bg-slate-950/60 text-xs uppercase text-slate-400 font-semibold tracking-wider">
            <tr>
              <th className="px-5 py-3.5">Asset</th>
              <th className="px-5 py-3.5 text-right">Quantity</th>
              <th className="px-5 py-3.5 text-right">Avg Price</th>
              <th className="px-5 py-3.5 text-right">Current Price</th>
              <th className="px-5 py-3.5 text-right">Current Value</th>
              <th className="px-5 py-3.5 text-right">Unrealized P&L</th>
              <th className="px-5 py-3.5 text-right">Allocation</th>
              <th className="px-5 py-3.5 text-center">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/80">
            {holdings.map((h) => {
              const isProfit = h.unrealizedPnL >= 0;
              const stockItem: StockResponse = {
                symbol: h.symbol,
                companyName: h.companyName,
                sector: 'Equities',
                currentPrice: h.currentPrice,
                previousClose: h.currentPrice,
                change: 0,
                changePercent: 0,
                dataStatus: 'DEMO',
                updatedAt: new Date().toISOString(),
              };

              return (
                <tr key={h.symbol} className="hover:bg-slate-850/50 transition">
                  <td className="px-5 py-4">
                    <div className="font-bold text-white">{h.symbol}</div>
                    <div className="text-xs text-slate-400 truncate max-w-xs">{h.companyName}</div>
                  </td>
                  <td className="px-5 py-4 text-right font-medium text-white">{h.quantity}</td>
                  <td className="px-5 py-4 text-right font-medium">{formatCurrency(h.averageBuyPrice)}</td>
                  <td className="px-5 py-4 text-right font-medium text-white">{formatCurrency(h.currentPrice)}</td>
                  <td className="px-5 py-4 text-right font-bold text-white">{formatCurrency(h.currentValue)}</td>
                  <td className="px-5 py-4 text-right font-bold">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded text-xs ${
                        isProfit
                          ? 'text-emerald-400 bg-emerald-950/60 border border-emerald-800/40'
                          : 'text-rose-400 bg-rose-950/60 border border-rose-800/40'
                      }`}
                    >
                      {formatCurrency(h.unrealizedPnL)}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-right font-medium text-slate-300">
                    {formatPercent(h.allocationPercent)}
                  </td>
                  <td className="px-5 py-4 text-center">
                    {onTradeStock && (
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => onTradeStock(stockItem, 'BUY')}
                          className="px-2.5 py-1 rounded-md text-xs font-semibold bg-emerald-950/80 text-emerald-300 border border-emerald-800/60 hover:bg-emerald-900 transition"
                        >
                          BUY
                        </button>
                        <button
                          onClick={() => onTradeStock(stockItem, 'SELL')}
                          className="px-2.5 py-1 rounded-md text-xs font-semibold bg-rose-950/80 text-rose-300 border border-rose-800/60 hover:bg-rose-900 transition"
                        >
                          SELL
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};