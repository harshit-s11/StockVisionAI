import { request } from './client';
import { HoldingResponse, PortfolioSummaryResponse, TransactionResponse } from '../types';

export const portfolioApi = {
  getSummary: () => request<PortfolioSummaryResponse>('/portfolio'),
  getHoldings: () => request<HoldingResponse[]>('/portfolio/holdings'),
  getTransactions: () => request<TransactionResponse[]>('/transactions'),
};