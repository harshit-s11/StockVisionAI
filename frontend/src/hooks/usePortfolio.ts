import { useState, useEffect, useCallback, useRef } from 'react';
import { HoldingResponse, PortfolioSummaryResponse, TransactionResponse } from '../types';
import { portfolioApi } from '../api/portfolioApi';

export interface UsePortfolioOptions {
  pollingIntervalMs?: number;
  autoRefresh?: boolean;
}

export function usePortfolio(options: UsePortfolioOptions = {}) {
  const { pollingIntervalMs = 15000, autoRefresh = true } = options;
  const [summary, setSummary] = useState<PortfolioSummaryResponse | null>(null);
  const [holdings, setHoldings] = useState<HoldingResponse[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef<boolean>(true);

  const fetchPortfolioData = useCallback(async (isBackground = false) => {
    if (!isBackground) {
      setIsLoading(true);
    }
    setError(null);
    try {
      const [sum, hld, txs] = await Promise.all([
        portfolioApi.getSummary(),
        portfolioApi.getHoldings(),
        portfolioApi.getTransactions(),
      ]);
      if (isMountedRef.current) {
        setSummary(sum);
        setHoldings(hld);
        setTransactions(txs);
      }
    } catch (err: unknown) {
      if (isMountedRef.current) {
        if (err && typeof err === 'object' && 'message' in err) {
          setError((err as { message: string }).message);
        } else {
          setError('Failed to load portfolio details');
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
    fetchPortfolioData(false);

    let intervalId: ReturnType<typeof setInterval> | null = null;
    if (autoRefresh && pollingIntervalMs > 0) {
      intervalId = setInterval(() => {
        fetchPortfolioData(true);
      }, pollingIntervalMs);
    }

    return () => {
      isMountedRef.current = false;
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [fetchPortfolioData, autoRefresh, pollingIntervalMs]);

  return {
    summary,
    holdings,
    transactions,
    isLoading,
    error,
    refresh: () => fetchPortfolioData(false),
  };
}