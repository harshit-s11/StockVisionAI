import { useState, useEffect, useCallback, useRef } from 'react';
import { StockResponse } from '../types';
import { marketApi } from '../api/marketApi';

export interface UseMarketDataOptions {
  pollingIntervalMs?: number;
  autoRefresh?: boolean;
}

export function useMarketData(options: UseMarketDataOptions = {}) {
  const { pollingIntervalMs = 15000, autoRefresh = true } = options;
  const [stocks, setStocks] = useState<StockResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef<boolean>(true);

  const fetchStocks = useCallback(async (isBackground = false) => {
    if (!isBackground) {
      setIsLoading(true);
    }
    setError(null);
    try {
      const data = await marketApi.getAllStocks();
      if (isMountedRef.current) {
        setStocks(data);
      }
    } catch (err: unknown) {
      if (isMountedRef.current) {
        if (err && typeof err === 'object' && 'message' in err) {
          setError((err as { message: string }).message);
        } else {
          setError('Failed to load market catalog');
        }
      }
    } finally {
      if (isMountedRef.current && !isBackground) {
        setIsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    isMountedRef.current = true;
    fetchStocks(false);

    let intervalId: ReturnType<typeof setInterval> | null = null;
    if (autoRefresh && pollingIntervalMs > 0) {
      intervalId = setInterval(() => {
        fetchStocks(true);
      }, pollingIntervalMs);
    }

    return () => {
      isMountedRef.current = false;
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [fetchStocks, autoRefresh, pollingIntervalMs]);

  return {
    stocks,
    isLoading,
    error,
    refresh: () => fetchStocks(false),
  };
}