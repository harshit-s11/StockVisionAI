import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
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
  {
    symbol: 'RELIANCE',
    companyName: 'Reliance Industries Ltd.',
    sector: 'Energy & Conglomerate',
    currentPrice: 2950.0,
    previousClose: 2920.0,
    change: 30.0,
    changePercent: 1.03,
    dataStatus: 'DEMO',
    updatedAt: new Date().toISOString(),
  },
  {
    symbol: 'HDFCBANK',
    companyName: 'HDFC Bank Ltd.',
    sector: 'Financial Services',
    currentPrice: 1480.0,
    previousClose: 1475.0,
    change: 5.0,
    changePercent: 0.34,
    dataStatus: 'DEMO',
    updatedAt: new Date().toISOString(),
  },
];

describe('Market Components', () => {
  it('renders stock catalog with DEMO badges and correct currency formatting', () => {
    render(<StockTable stocks={mockStocks} onTrade={vi.fn()} onAnalyze={vi.fn()} />);

    expect(screen.getByText('TCS')).toBeInTheDocument();
    expect(screen.getByText('INFY')).toBeInTheDocument();
    expect(screen.getByText('RELIANCE')).toBeInTheDocument();
    expect(screen.getByText('HDFCBANK')).toBeInTheDocument();
    expect(screen.getAllByText('DEMO')).toHaveLength(4);
    expect(screen.getByText('₹3,850.00')).toBeInTheDocument();
  });

  it('filters stocks by search keyword matching symbol or company name (case-insensitive)', () => {
    render(<StockTable stocks={mockStocks} onTrade={vi.fn()} onAnalyze={vi.fn()} />);

    const searchInput = screen.getByPlaceholderText(/search equities/i);
    fireEvent.change(searchInput, { target: { value: 'tata' } });

    expect(screen.getByText('TCS')).toBeInTheDocument();
    expect(screen.queryByText('INFY')).not.toBeInTheDocument();
    expect(screen.queryByText('RELIANCE')).not.toBeInTheDocument();

    // Clear search
    const clearBtn = screen.getByRole('button', { name: /clear search/i });
    fireEvent.click(clearBtn);

    expect(screen.getByText('INFY')).toBeInTheDocument();
  });

  it('filters stocks dynamically by sector button', () => {
    render(<StockTable stocks={mockStocks} onTrade={vi.fn()} onAnalyze={vi.fn()} />);

    // Click on Financial Services sector button
    const finBtn = screen.getByRole('button', { name: 'Financial Services' });
    fireEvent.click(finBtn);

    expect(screen.getByText('HDFCBANK')).toBeInTheDocument();
    expect(screen.queryByText('TCS')).not.toBeInTheDocument();
    expect(screen.queryByText('RELIANCE')).not.toBeInTheDocument();

    // Click All button
    const allBtn = screen.getByRole('button', { name: 'All' });
    fireEvent.click(allBtn);

    expect(screen.getByText('TCS')).toBeInTheDocument();
    expect(screen.getByText('RELIANCE')).toBeInTheDocument();
  });

  it('filters stocks using combined search and sector criteria (AND logic)', () => {
    render(<StockTable stocks={mockStocks} onTrade={vi.fn()} onAnalyze={vi.fn()} />);

    const itSectorBtn = screen.getByRole('button', { name: 'Information Technology' });
    fireEvent.click(itSectorBtn);

    const searchInput = screen.getByPlaceholderText(/search equities/i);
    fireEvent.change(searchInput, { target: { value: 'infosys' } });

    expect(screen.getByText('INFY')).toBeInTheDocument();
    expect(screen.queryByText('TCS')).not.toBeInTheDocument();
    expect(screen.queryByText('RELIANCE')).not.toBeInTheDocument();
  });

  it('renders empty search state and resets filters when no equities match', () => {
    render(<StockTable stocks={mockStocks} onTrade={vi.fn()} onAnalyze={vi.fn()} />);

    const searchInput = screen.getByPlaceholderText(/search equities/i);
    fireEvent.change(searchInput, { target: { value: 'non_existent_ticker' } });

    expect(screen.getByText(/no equities match your search or filter criteria/i)).toBeInTheDocument();

    const resetBtn = screen.getByRole('button', { name: /reset filters/i });
    fireEvent.click(resetBtn);

    expect(screen.getByText('TCS')).toBeInTheDocument();
    expect(screen.getByText('INFY')).toBeInTheDocument();
  });
});