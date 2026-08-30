import { useState, useEffect, useCallback } from 'react';
import { StockResponse } from '../types';
import { marketApi } from '../api/marketApi';

export function useMarketData() {
  const [stocks, setStocks] = useState<StockResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStocks = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await marketApi.getAllStocks();
      setStocks(data);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'message' in err) {
        setError((err as { message: string }).message);
      } else {
        setError('Failed to load market catalog');
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStocks();
  }, [fetchStocks]);

  return {
    stocks,
    isLoading,
    error,
    refresh: fetchStocks,
  };
}