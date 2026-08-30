import React from 'react';
import { usePortfolio } from '../hooks/usePortfolio';
import { formatCurrency, formatDateTime } from '../utils/formatters';
import { Badge } from '../components/common/Badge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { History, RefreshCw } from 'lucide-react';

export const TransactionsPage: React.FC = () => {
  const { transactions, isLoading, error, refresh } = usePortfolio();

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <History className="h-6 w-6 text-emerald-400" />
            <h1 className="text-2xl font-bold text-white tracking-tight">Trade History Logs</h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Immutable audit record of executed BUY and SELL orders (Sorted newest first, top 50)
          </p>
        </div>
        <button
          onClick={refresh}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-800 bg-slate-900 text-xs font-semibold text-slate-300 hover:text-white hover:bg-slate-800 transition"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          <span>Refresh</span>
        </button>
      </div>

      {error && <ErrorAlert message={error} />}

      {isLoading && transactions.length === 0 ? (
        <LoadingSpinner size="lg" message="Loading transaction logs..." />
      ) : transactions.length === 0 ? (
        <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-12 text-center space-y-2">
          <div className="text-slate-400 font-semibold text-base">No transaction records found</div>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Your trade executions will automatically appear here once you place a BUY or SELL order.
          </p>
        </div>
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="border-b border-slate-800 bg-slate-950/60 text-xs uppercase text-slate-400 font-semibold tracking-wider">
                <tr>
                  <th className="px-5 py-3.5">Timestamp</th>
                  <th className="px-5 py-3.5">Symbol</th>
                  <th className="px-5 py-3.5 text-center">Type</th>
                  <th className="px-5 py-3.5 text-right">Quantity</th>
                  <th className="px-5 py-3.5 text-right">Execution Price</th>
                  <th className="px-5 py-3.5 text-right">Total Trade Value</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-slate-850/50 transition">
                    <td className="px-5 py-4 text-xs font-medium text-slate-400">
                      {formatDateTime(tx.timestamp)}
                    </td>
                    <td className="px-5 py-4 font-bold text-white">{tx.symbol}</td>
                    <td className="px-5 py-4 text-center">
                      <Badge variant={tx.type === 'BUY' ? 'buy' : 'sell'}>{tx.type}</Badge>
                    </td>
                    <td className="px-5 py-4 text-right font-semibold text-white">{tx.quantity}</td>
                    <td className="px-5 py-4 text-right font-medium">{formatCurrency(tx.executionPrice)}</td>
                    <td className="px-5 py-4 text-right font-bold text-white">
                      {formatCurrency(tx.totalValue)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};