import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AiChatPanel } from '../components/ai/AiChatPanel';
import { MarkdownRenderer } from '../components/common/MarkdownRenderer';
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
      expect(screen.getByText('Guide')).toBeInTheDocument();
    });
  });

  it('renders rich Markdown elements (headings, bold, lists) without raw markers', async () => {
    (aiApi.chat as any).mockResolvedValueOnce({
      answer: `### Portfolio Allocation Summary\n\nYour portfolio is **well diversified** across sectors.\n\n* Information Technology: 40%\n* Banking: 60%\n\n---`,
      disclaimer: 'Educational information only.',
      sources: [
        { title: 'Asset Allocation Framework', source: 'Education' },
        { title: 'Risk Management 101', source: 'Handbook' },
      ],
    });

    render(<AiChatPanel />);

    const presetBtn = screen.getByText('How diversified is my portfolio?');
    fireEvent.click(presetBtn);

    await waitFor(() => {
      // Check heading h3
      const heading = screen.getByRole('heading', { level: 3, name: /Portfolio Allocation Summary/i });
      expect(heading).toBeInTheDocument();

      // Check bold text
      const strongText = screen.getByText('well diversified');
      expect(strongText.tagName).toBe('STRONG');

      // Check list items
      const itItem = screen.getByText(/Information Technology: 40%/i);
      expect(itItem.tagName).toBe('LI');

      // Check both source chips rendered separately
      expect(screen.getByText('Asset Allocation Framework')).toBeInTheDocument();
      expect(screen.getByText('Risk Management 101')).toBeInTheDocument();
      expect(screen.getByText('Education')).toBeInTheDocument();
      expect(screen.getByText('Handbook')).toBeInTheDocument();
    });
  });

  it('sanitizes stray SVG code blocks and raw svg tags in MarkdownRenderer', () => {
    const rawMarkdown = `### Asset Breakdown\n\n\`\`\`svg\n<svg viewBox="0 0 100 100"><circle cx="50" cy="50" r="40"/></svg>\n\`\`\`\n\n<svg><rect width="10" height="10"/></svg>\n\nValid text explaining allocation.`;

    const { container } = render(<MarkdownRenderer content={rawMarkdown} />);

    expect(screen.getByRole('heading', { level: 3, name: /Asset Breakdown/i })).toBeInTheDocument();
    expect(screen.getByText(/Valid text explaining allocation/i)).toBeInTheDocument();

    // Verify no svg elements exist in the rendered output
    expect(container.querySelector('svg')).toBeNull();
    // Verify no literal "```svg" or "<svg>" text was rendered
    expect(container.textContent).not.toContain('```svg');
    expect(container.textContent).not.toContain('<svg');
  });
});
