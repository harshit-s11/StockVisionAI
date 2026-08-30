import { request } from './client';
import { TradeRequest, TradeResponse } from '../types';

export const tradeApi = {
  buy: (data: TradeRequest) =>
    request<TradeResponse>('/trades/buy', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  sell: (data: TradeRequest) =>
    request<TradeResponse>('/trades/sell', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
};