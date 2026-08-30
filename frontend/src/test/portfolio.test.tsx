import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { HoldingsTable } from '../components/portfolio/HoldingsTable';
import { MetricCard } from '../components/common/MetricCard';
import { HoldingResponse } from '../types';
import { TrendingUp } from 'lucide-react';

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

describe('Portfolio Components', () => {
  it('renders metric card with profit styling and percentage', () => {
    render(
      <MetricCard
        label="Total Portfolio Value"
        value="₹1,38,500.00"
        change={38.5}
        changeLabel="overall"
        icon={TrendingUp}
      />
    );

    expect(screen.getByText('Total Portfolio Value')).toBeInTheDocument();
    expect(screen.getByText('₹1,38,500.00')).toBeInTheDocument();
    expect(screen.getByText('+38.50% overall')).toBeInTheDocument();
  });

  it('renders holdings table with active positions', () => {
    render(<HoldingsTable holdings={mockHoldings} onTradeStock={vi.fn()} />);

    expect(screen.getByText('TCS')).toBeInTheDocument();
    expect(screen.getByText('10')).toBeInTheDocument();
    expect(screen.getByText('₹38,500.00')).toBeInTheDocument();
    expect(screen.getByText('₹500.00')).toBeInTheDocument();
  });

  it('renders empty holdings state when portfolio has no positions', () => {
    render(<HoldingsTable holdings={[]} onTradeStock={vi.fn()} />);

    expect(screen.getByText(/No active holdings in your portfolio/i)).toBeInTheDocument();
  });
});