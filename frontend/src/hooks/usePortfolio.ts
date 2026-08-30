import { useState, useEffect, useCallback } from 'react';
import { HoldingResponse, PortfolioSummaryResponse, TransactionResponse } from '../types';
import { portfolioApi } from '../api/portfolioApi';

export function usePortfolio() {
  const [summary, setSummary] = useState<PortfolioSummaryResponse | null>(null);
  const [holdings, setHoldings] = useState<HoldingResponse[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPortfolioData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [sum, hld, txs] = await Promise.all([
        portfolioApi.getSummary(),
        portfolioApi.getHoldings(),
        portfolioApi.getTransactions(),
      ]);
      setSummary(sum);
      setHoldings(hld);
      setTransactions(txs);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'message' in err) {
        setError((err as { message: string }).message);
      } else {
        setError('Failed to load portfolio details');
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPortfolioData();
  }, [fetchPortfolioData]);

  return {
    summary,
    holdings,
    transactions,
    isLoading,
    error,
    refresh: fetchPortfolioData,
  };
}