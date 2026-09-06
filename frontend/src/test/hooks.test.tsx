import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useMarketData } from '../hooks/useMarketData';
import { usePortfolio } from '../hooks/usePortfolio';
import { marketApi } from '../api/marketApi';
import { portfolioApi } from '../api/portfolioApi';
import { StockResponse, PortfolioSummaryResponse, HoldingResponse, TransactionResponse } from '../types';

vi.mock('../api/marketApi');
vi.mock('../api/portfolioApi');

const mockStocks: StockResponse[] = [
  {
    symbol: 'TCS',
    companyName: 'Tata Consultancy Services Ltd.',
    sector: 'Information Technology',
    currentPrice: 3850.0,
    previousClose: 3810.0,
    change: 40.0,
    changePercent: 1.05,
    dataStatus: 'DEMO',
    updatedAt: new Date().toISOString(),
  },
];

const mockSummary: PortfolioSummaryResponse = {
  cashBalance: 61500.0,
  totalInvested: 38000.0,
  currentHoldingsValue: 38500.0,
  totalPortfolioValue: 100000.0,
  unrealizedPnL: 500.0,
  unrealizedPnLPercent: 1.32,
  portfolioReturnPercent: 0.0,
};

const mockHoldings: HoldingResponse[] = [
  {
    symbol: 'TCS',
    companyName: 'Tata Consultancy Services Ltd.',
    quantity: 10,
    averageBuyPrice: 3800.0,
    currentPrice: 3850.0,
    currentValue: 38500.0,
    unrealizedPnL: 500.0,
    allocationPercent: 38.5,
  },
];

const mockTransactions: TransactionResponse[] = [];

describe('Market and Portfolio Polling Hooks', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.mocked(marketApi.getAllStocks).mockResolvedValue(mockStocks);
    vi.mocked(portfolioApi.getSummary).mockResolvedValue(mockSummary);
    vi.mocked(portfolioApi.getHoldings).mockResolvedValue(mockHoldings);
    vi.mocked(portfolioApi.getTransactions).mockResolvedValue(mockTransactions);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('useMarketData fetches initial stocks and polls periodically', async () => {
    const { result, unmount } = renderHook(() =>
      useMarketData({ pollingIntervalMs: 5000, autoRefresh: true })
    );

    // Initial load
    expect(result.current.isLoading).toBe(true);
    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.stocks).toHaveLength(1);
    expect(marketApi.getAllStocks).toHaveBeenCalledTimes(1);

    // Advance timer by 5000ms
    const updatedStocks: StockResponse[] = [
      { ...mockStocks[0], currentPrice: 3870.0 },
    ];
    vi.mocked(marketApi.getAllStocks).mockResolvedValueOnce(updatedStocks);

    await act(async () => {
      vi.advanceTimersByTime(5000);
      await Promise.resolve();
    });

    expect(marketApi.getAllStocks).toHaveBeenCalledTimes(2);
    expect(result.current.stocks[0].currentPrice).toBe(3870.0);
    // Background polling should NOT reset isLoading to true
    expect(result.current.isLoading).toBe(false);

    // Unmount hook and advance timer
    unmount();
    await act(async () => {
      vi.advanceTimersByTime(10000);
      await Promise.resolve();
    });

    // Should not have polled after unmount
    expect(marketApi.getAllStocks).toHaveBeenCalledTimes(2);
  });

  it('usePortfolio fetches initial data and polls periodically', async () => {
    const { result, unmount } = renderHook(() =>
      usePortfolio({ pollingIntervalMs: 5000, autoRefresh: true })
    );

    expect(result.current.isLoading).toBe(true);
    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.summary?.totalPortfolioValue).toBe(100000.0);
    expect(portfolioApi.getSummary).toHaveBeenCalledTimes(1);

    // Advance timer by 5000ms with updated valuation
    const updatedSummary = { ...mockSummary, totalPortfolioValue: 101000.0 };
    vi.mocked(portfolioApi.getSummary).mockResolvedValueOnce(updatedSummary);

    await act(async () => {
      vi.advanceTimersByTime(5000);
      await Promise.resolve();
    });

    expect(portfolioApi.getSummary).toHaveBeenCalledTimes(2);
    expect(result.current.summary?.totalPortfolioValue).toBe(101000.0);
    expect(result.current.isLoading).toBe(false);

    unmount();
    await act(async () => {
      vi.advanceTimersByTime(10000);
      await Promise.resolve();
    });

    expect(portfolioApi.getSummary).toHaveBeenCalledTimes(2);
  });
});