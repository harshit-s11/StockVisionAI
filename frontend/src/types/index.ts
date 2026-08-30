export interface UserResponse {
  id: string;
  username: string;
  email: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface StockResponse {
  symbol: string;
  companyName: string;
  sector: string;
  currentPrice: number;
  previousClose: number;
  change: number;
  changePercent: number;
  dataStatus: string;
  updatedAt: string;
}

export interface PortfolioSummaryResponse {
  cashBalance: number;
  totalInvested: number;
  currentHoldingsValue: number;
  totalPortfolioValue: number;
  unrealizedPnL: number;
  unrealizedPnLPercent: number;
  portfolioReturnPercent: number;
}

export interface HoldingResponse {
  symbol: string;
  companyName: string;
  quantity: number;
  averageBuyPrice: number;
  currentPrice: number;
  currentValue: number;
  unrealizedPnL: number;
  allocationPercent: number;
}

export interface TransactionResponse {
  id: string;
  symbol: string;
  type: 'BUY' | 'SELL';
  quantity: number;
  executionPrice: number;
  totalValue: number;
  timestamp: string;
}

export interface TradeRequest {
  symbol: string;
  quantity: number;
}

export interface TradeResponse {
  transactionId: string;
  symbol: string;
  type: 'BUY' | 'SELL';
  quantity: number;
  executionPrice: number;
  totalValue: number;
  cashBalance: number;
  holdingQuantity: number;
  averageBuyPrice: number;
  timestamp: string;
}

export interface KnowledgeSourceResponse {
  title: string;
  source: string;
}

export interface AiChatRequest {
  message: string;
}

export interface AiChatResponse {
  answer: string;
  disclaimer: string;
  sources: KnowledgeSourceResponse[];
}

export interface StockAnalysisRequest {
  symbol: string;
}

export interface StockAnalysisResponse {
  symbol: string;
  analysis: string;
  keyFactors: string[];
  risks: string[];
  disclaimer: string;
  sources: KnowledgeSourceResponse[];
}

export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
}