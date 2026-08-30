import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AiChatPanel } from '../components/ai/AiChatPanel';
import { aiApi } from '../api/aiApi';

vi.mock('../api/aiApi', () => ({
  aiApi: {
    chat: vi.fn(),
  },
}));

describe('AI Advisor UI', () => {
  it('renders chat preset and submits message, displaying answer and disclaimer', async () => {
    (aiApi.chat as any).mockResolvedValueOnce({
      answer: 'Your portfolio is 100% in cash liquidity with ₹100,000 capital.',
      disclaimer: 'Educational information only. Not financial advice. Demo market data.',
      sources: [{ title: 'Portfolio Diversification', source: 'Guide' }],
    });

    render(<AiChatPanel />);

    const presetBtn = screen.getByText('How diversified is my portfolio?');
    fireEvent.click(presetBtn);

    await waitFor(() => {
      expect(aiApi.chat).toHaveBeenCalledWith({
        message: 'How diversified is my portfolio?',
      });
      expect(screen.getByText(/Your portfolio is 100% in cash/i)).toBeInTheDocument();
      expect(screen.getByText(/Educational information only/i)).toBeInTheDocument();
      expect(screen.getByText('Portfolio Diversification')).toBeInTheDocument();
    });
  });
});