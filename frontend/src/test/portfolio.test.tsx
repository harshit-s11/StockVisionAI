import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { HoldingsTable } from '../components/portfolio/HoldingsTable';
import { AllocationChart } from '../components/portfolio/AllocationChart';
import { MetricCard } from '../components/common/MetricCard';
import { HoldingResponse, PortfolioSummaryResponse, StockResponse } from '../types';
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

const mockSummary: PortfolioSummaryResponse = {
  cashBalance: 61500.0,
  totalInvested: 38000.0,
  currentHoldingsValue: 38500.0,
  totalPortfolioValue: 100000.0,
  unrealizedPnL: 500.0,
  unrealizedPnLPercent: 1.32,
  portfolioReturnPercent: 0.0,
};

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

  it('renders AllocationChart with valid portfolio allocation data and toggles view mode', () => {
    render(
      <AllocationChart
        summary={mockSummary}
        holdings={mockHoldings}
        stocks={mockStocks}
      />
    );

    // Verify donut center value and positions
    expect(screen.getByText('Total Value')).toBeInTheDocument();
    expect(screen.getByText('₹1,00,000.00')).toBeInTheDocument();
    expect(screen.getByText('1 Active Positions')).toBeInTheDocument();

    // Verify holding items in breakdown
    expect(screen.getByText('Cash Balance')).toBeInTheDocument();
    expect(screen.getByText('₹61,500.00')).toBeInTheDocument();
    expect(screen.getAllByText('61.50%').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('TCS')).toBeInTheDocument();
    expect(screen.getByText('₹38,500.00')).toBeInTheDocument();
    expect(screen.getAllByText('38.50%').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Information Technology')).toBeInTheDocument();

    // Verify SVG geometry (no NaN in circle attributes)
    const circles = document.querySelectorAll('svg circle');
    expect(circles.length).toBeGreaterThanOrEqual(2);
    circles.forEach((circle) => {
      const strokeDash = circle.getAttribute('stroke-dasharray');
      if (strokeDash) {
        expect(strokeDash).not.toContain('NaN');
      }
    });

    // Toggle view mode to Asset Class
    const assetClassBtn = screen.getByRole('button', { name: /asset class/i });
    fireEvent.click(assetClassBtn);

    expect(screen.getByText('Cash Reserves')).toBeInTheDocument();
    expect(screen.getByText('Equities Portfolio')).toBeInTheDocument();
  });

  it('renders AllocationChart gracefully when portfolio is 100% cash', () => {
    const emptySummary: PortfolioSummaryResponse = {
      cashBalance: 100000.0,
      totalInvested: 0.0,
      currentHoldingsValue: 0.0,
      totalPortfolioValue: 100000.0,
      unrealizedPnL: 0.0,
      unrealizedPnLPercent: 0.0,
      portfolioReturnPercent: 0.0,
    };

    render(<AllocationChart summary={emptySummary} holdings={[]} />);

    expect(screen.getByText('100% Cash Liquidity')).toBeInTheDocument();
    expect(screen.getByText(/Your portfolio currently holds 100% cash reserves/i)).toBeInTheDocument();

    const circles = document.querySelectorAll('svg circle');
    expect(circles.length).toBeGreaterThanOrEqual(1);
    circles.forEach((circle) => {
      const strokeDash = circle.getAttribute('stroke-dasharray');
      if (strokeDash) {
        expect(strokeDash).not.toContain('NaN');
      }
    });
  });
});