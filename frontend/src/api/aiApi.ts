import { request } from './client';
import { AiChatRequest, AiChatResponse, StockAnalysisRequest, StockAnalysisResponse } from '../types';

export const aiApi = {
  chat: (data: AiChatRequest) =>
    request<AiChatResponse>('/ai/chat', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  analyzeStock: (data: StockAnalysisRequest) =>
    request<StockAnalysisResponse>('/ai/analyze-stock', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
};