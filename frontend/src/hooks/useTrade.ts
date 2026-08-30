import { useState } from 'react';
import { TradeRequest, TradeResponse } from '../types';
import { tradeApi } from '../api/tradeApi';

export function useTrade(onSuccess?: () => void) {
  const [isTrading, setIsTrading] = useState<boolean>(false);
  const [tradeError, setTradeError] = useState<string | null>(null);
  const [lastTrade, setLastTrade] = useState<TradeResponse | null>(null);

  const executeTrade = async (type: 'BUY' | 'SELL', request: TradeRequest) => {
    setIsTrading(true);
    setTradeError(null);
    try {
      const response = type === 'BUY'
        ? await tradeApi.buy(request)
        : await tradeApi.sell(request);

      setLastTrade(response);
      if (onSuccess) {
        onSuccess();
      }
      return response;
    } catch (err: unknown) {
      const message = (err && typeof err === 'object' && 'message' in err)
        ? (err as { message: string }).message
        : 'Trade execution failed';
      setTradeError(message);
      throw err;
    } finally {
      setIsTrading(false);
    }
  };

  const clearTradeState = () => {
    setTradeError(null);
    setLastTrade(null);
  };

  return {
    executeTrade,
    isTrading,
    tradeError,
    lastTrade,
    clearTradeState,
  };
}