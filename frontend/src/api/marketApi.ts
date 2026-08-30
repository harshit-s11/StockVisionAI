import { request } from './client';
import { StockResponse } from '../types';

export const marketApi = {
  getAllStocks: () => request<StockResponse[]>('/stocks'),
  getStockBySymbol: (symbol: string) => request<StockResponse>(`/stocks/${encodeURIComponent(symbol)}`),
};