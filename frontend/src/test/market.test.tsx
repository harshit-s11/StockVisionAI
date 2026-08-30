import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StockTable } from '../components/market/StockTable';
import { StockResponse } from '../types';

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
  {
    symbol: 'INFY',
    companyName: 'Infosys Ltd.',
    sector: 'Information Technology',
    currentPrice: 1620.0,
    previousClose: 1605.0,
    change: 15.0,
    changePercent: 0.93,
    dataStatus: 'DEMO',
    updatedAt: new Date().toISOString(),
  },
];

describe('Market Components', () => {
  it('renders stock catalog with DEMO badges and correct currency formatting', () => {
    render(<StockTable stocks={mockStocks} onTrade={vi.fn()} onAnalyze={vi.fn()} />);

    expect(screen.getByText('TCS')).toBeInTheDocument();
    expect(screen.getByText('INFY')).toBeInTheDocument();
    expect(screen.getAllByText('DEMO')).toHaveLength(2);
    expect(screen.getByText('₹3,850.00')).toBeInTheDocument();
  });
});