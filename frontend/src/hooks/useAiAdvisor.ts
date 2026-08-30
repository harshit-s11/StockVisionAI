import { useState } from 'react';
import { AiChatResponse, StockAnalysisResponse } from '../types';
import { aiApi } from '../api/aiApi';

export function useAiAdvisor() {
  const [isAiLoading, setIsAiLoading] = useState<boolean>(false);
  const [aiError, setAiError] = useState<string | null>(null);

  const askChat = async (message: string): Promise<AiChatResponse> => {
    setIsAiLoading(true);
    setAiError(null);
    try {
      return await aiApi.chat({ message });
    } catch (err: unknown) {
      const message = (err && typeof err === 'object' && 'message' in err)
        ? (err as { message: string }).message
        : 'Failed to contact AI advisor';
      setAiError(message);
      throw err;
    } finally {
      setIsAiLoading(false);
    }
  };

  const analyzeStock = async (symbol: string): Promise<StockAnalysisResponse> => {
    setIsAiLoading(true);
    setAiError(null);
    try {
      return await aiApi.analyzeStock({ symbol });
    } catch (err: unknown) {
      const message = (err && typeof err === 'object' && 'message' in err)
        ? (err as { message: string }).message
        : 'Failed to analyze stock with AI';
      setAiError(message);
      throw err;
    } finally {
      setIsAiLoading(false);
    }
  };

  return {
    askChat,
    analyzeStock,
    isAiLoading,
    aiError,
  };
}