import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TradeModal } from '../components/trading/TradeModal';
import { StockResponse } from '../types';
import { tradeApi } from '../api/tradeApi';

vi.mock('../api/tradeApi', () => ({
  tradeApi: {
    buy: vi.fn(),
    sell: vi.fn(),
  },
}));

const mockStock: StockResponse = {
  symbol: 'TCS',
  companyName: 'Tata Consultancy Services Ltd.',
  sector: 'Information Technology',
  currentPrice: 3850.0,
  previousClose: 3810.0,
  change: 40.0,
  changePercent: 1.05,
  dataStatus: 'DEMO',
  updatedAt: new Date().toISOString(),
};

describe('Trading UI & Modal', () => {
  it('submits BUY request with strictly symbol and quantity', async () => {
    (tradeApi.buy as any).mockResolvedValueOnce({
      transactionId: 'tx-123',
      symbol: 'TCS',
      type: 'BUY',
      quantity: 5,
      executionPrice: 3850.0,
      totalValue: 19250.0,
      cashBalance: 80750.0,
      holdingQuantity: 5,
      averageBuyPrice: 3850.0,
      timestamp: new Date().toISOString(),
    });

    const onComplete = vi.fn();
    const onClose = vi.fn();

    render(
      <TradeModal
        isOpen={true}
        stock={mockStock}
        initialType="BUY"
        availableCash={100000}
        maxSellQuantity={0}
        onClose={onClose}
        onTradeComplete={onComplete}
      />
    );

    const qtyInput = screen.getByLabelText(/quantity/i);
    fireEvent.change(qtyInput, { target: { value: '5' } });

    const confirmBtn = screen.getByRole('button', { name: /confirm buy/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(tradeApi.buy).toHaveBeenCalledWith({
        symbol: 'TCS',
        quantity: 5,
      });
      expect(onComplete).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });
});