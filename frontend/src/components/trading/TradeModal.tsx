import React, { useState, useEffect } from 'react';
import { X, ArrowRight, ShieldCheck } from 'lucide-react';
import { StockResponse, TradeResponse } from '../../types';
import { formatCurrency } from '../../utils/formatters';
import { useTrade } from '../../hooks/useTrade';
import { ErrorAlert } from '../common/ErrorAlert';
import { LoadingSpinner } from '../common/LoadingSpinner';

interface TradeModalProps {
  isOpen: boolean;
  stock: StockResponse | null;
  initialType?: 'BUY' | 'SELL';
  maxSellQuantity?: number;
  availableCash?: number;
  onClose: () => void;
  onTradeComplete?: (response: TradeResponse) => void;
}

export const TradeModal: React.FC<TradeModalProps> = ({
  isOpen,
  stock,
  initialType = 'BUY',
  maxSellQuantity = 0,
  availableCash = 0,
  onClose,
  onTradeComplete,
}) => {
  const [tradeType, setTradeType] = useState<'BUY' | 'SELL'>(initialType);
  const [quantity, setQuantity] = useState<number>(1);
  const { executeTrade, isTrading, tradeError, clearTradeState } = useTrade();

  useEffect(() => {
    setTradeType(initialType);
    setQuantity(1);
    clearTradeState();
  }, [isOpen, initialType, stock]);

  if (!isOpen || !stock) return null;

  const currentPrice = Number(stock.currentPrice);
  const estimatedTotal = (Number(quantity) || 0) * currentPrice;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!quantity || quantity <= 0) return;

    try {
      const response = await executeTrade(tradeType, {
        symbol: stock.symbol,
        quantity: Math.floor(quantity),
      });
      if (onTradeComplete) {
        onTradeComplete(response);
      }
      onClose();
    } catch {
      // Error managed by useTrade hook
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
      aria-labelledby="trade-dialog-title"
    >
      <div className="relative w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl space-y-6">
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-4">
          <div>
            <h2 id="trade-dialog-title" className="text-lg font-bold text-white flex items-center gap-2">
              <span>Execute Order</span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                {stock.symbol}
              </span>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">{stock.companyName}</p>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 transition"
            aria-label="Close dialog"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {tradeError && <ErrorAlert message={tradeError} onDismiss={clearTradeState} />}

        {/* Order Type Toggle */}
        <div className="grid grid-cols-2 gap-2 bg-slate-950/60 p-1.5 rounded-xl border border-slate-800">
          <button
            type="button"
            onClick={() => setTradeType('BUY')}
            className={`py-2 rounded-lg text-sm font-semibold transition ${
              tradeType === 'BUY'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            BUY
          </button>
          <button
            type="button"
            onClick={() => setTradeType('SELL')}
            className={`py-2 rounded-lg text-sm font-semibold transition ${
              tradeType === 'SELL'
                ? 'bg-rose-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            SELL
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <div className="flex justify-between items-center mb-1 text-xs">
              <label htmlFor="trade-quantity" className="font-semibold text-slate-300">
                Quantity (Whole Shares)
              </label>
              <span className="text-slate-400">
                {tradeType === 'BUY'
                  ? `Available Cash: ${formatCurrency(availableCash)}`
                  : `Owned Shares: ${maxSellQuantity}`}
              </span>
            </div>
            <input
              id="trade-quantity"
              type="number"
              min="1"
              step="1"
              value={quantity || ''}
              onChange={(e) => setQuantity(parseInt(e.target.value, 10) || 0)}
              className="w-full rounded-xl border border-slate-800 bg-slate-950 px-4 py-2.5 text-white placeholder-slate-500 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 text-base font-semibold"
              placeholder="e.g. 5"
              required
            />
          </div>

          <div className="rounded-xl border border-slate-800/80 bg-slate-950/40 p-4 space-y-2 text-sm">
            <div className="flex justify-between text-slate-400 text-xs">
              <span>Server-Resolved Price:</span>
              <span className="font-medium text-white">{formatCurrency(currentPrice)}</span>
            </div>
            <div className="flex justify-between text-slate-400 text-xs">
              <span>Order Type:</span>
              <span className="font-medium text-slate-300">Market Order (Demo Feed)</span>
            </div>
            <div className="border-t border-slate-800 pt-2 flex justify-between font-bold text-base text-white">
              <span>Estimated Total:</span>
              <span className={tradeType === 'BUY' ? 'text-emerald-400' : 'text-rose-400'}>
                {formatCurrency(estimatedTotal)}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2 text-xs text-slate-400 px-1">
            <ShieldCheck className="h-4 w-4 text-emerald-400 shrink-0" />
            <span>Pessimistic locking enforced on server transaction.</span>
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="w-1/3 py-2.5 rounded-xl border border-slate-800 bg-slate-800/60 font-semibold text-slate-300 hover:bg-slate-800 transition text-sm"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isTrading || quantity <= 0}
              className={`w-2/3 py-2.5 rounded-xl font-bold text-white transition flex items-center justify-center gap-2 text-sm shadow-lg ${
                tradeType === 'BUY'
                  ? 'bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-950 disabled:text-slate-500'
                  : 'bg-rose-600 hover:bg-rose-500 disabled:bg-rose-950 disabled:text-slate-500'
              }`}
            >
              {isTrading ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <span>Confirm {tradeType}</span>
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};