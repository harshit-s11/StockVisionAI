import React, { useState } from 'react';
import { Send, Sparkles, BookOpen, AlertTriangle } from 'lucide-react';
import { useAiAdvisor } from '../../hooks/useAiAdvisor';
import { AiChatResponse } from '../../types';
import { ErrorAlert } from '../common/ErrorAlert';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const AiChatPanel: React.FC = () => {
  const [inputMessage, setInputMessage] = useState('');
  const [chatHistory, setChatHistory] = useState<Array<{ role: 'user' | 'assistant'; text: string; data?: AiChatResponse }>>([]);
  const { askChat, isAiLoading, aiError } = useAiAdvisor();

  const presets = [
    'How diversified is my portfolio?',
    'Explain P/E ratio and how it applies to tech equities',
    'What is position sizing in risk management?',
    'What is the difference between value and momentum investing?',
  ];

  const handleSend = async (msgToSend?: string) => {
    const text = (msgToSend || inputMessage).trim();
    if (!text || isAiLoading) return;

    setChatHistory((prev) => [...prev, { role: 'user', text }]);
    setInputMessage('');

    try {
      const response = await askChat(text);
      setChatHistory((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: response.answer,
          data: response,
        },
      ]);
    } catch {
      // Error handled by hook
    }
  };

  return (
    <div className="flex flex-col h-[650px] rounded-2xl border border-slate-800 bg-slate-900/70 overflow-hidden shadow-xl">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-800/80 px-6 py-4 bg-slate-950/60">
        <div className="flex items-center gap-2.5">
          <div className="h-8 w-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
            <Sparkles className="h-4 w-4" />
          </div>
          <div>
            <h2 className="font-bold text-sm text-white">StockVision AI Advisor</h2>
            <p className="text-xs text-slate-400">Grounding your live portfolio metrics with financial knowledge</p>
          </div>
        </div>
        <div className="text-xs px-2.5 py-1 rounded-md bg-slate-800 text-slate-300 font-medium">
          RAG + Gemini
        </div>
      </div>

      {/* Message Feed */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {chatHistory.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center p-6 space-y-4">
            <div className="h-12 w-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Sparkles className="h-6 w-6" />
            </div>
            <div className="space-y-1">
              <h3 className="font-bold text-base text-white">Interactive Financial Advisor</h3>
              <p className="text-xs text-slate-400 max-w-md mx-auto">
                Ask questions about your portfolio allocation, risk management, market metrics, or technical indicators.
              </p>
            </div>

            {/* Presets */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full max-w-lg pt-2">
              {presets.map((preset) => (
                <button
                  key={preset}
                  onClick={() => handleSend(preset)}
                  className="p-3 text-left rounded-xl border border-slate-800/80 bg-slate-950/40 hover:bg-slate-800 text-xs font-medium text-slate-300 hover:text-white transition"
                >
                  {preset}
                </button>
              ))}
            </div>
          </div>
        ) : (
          chatHistory.map((item, idx) => (
            <div
              key={idx}
              className={`flex flex-col ${item.role === 'user' ? 'items-end' : 'items-start'} space-y-1.5`}
            >
              <div
                className={`max-w-2xl rounded-2xl px-5 py-3.5 text-sm leading-relaxed ${
                  item.role === 'user'
                    ? 'bg-emerald-600 text-white font-medium rounded-br-none'
                    : 'bg-slate-950 border border-slate-800 text-slate-200 rounded-bl-none shadow-sm'
                }`}
              >
                {item.text}
              </div>

              {item.data && (
                <div className="max-w-2xl w-full space-y-2 text-xs">
                  {item.data.sources && item.data.sources.length > 0 && (
                    <div className="flex flex-wrap items-center gap-1.5 text-slate-400">
                      <BookOpen className="h-3.5 w-3.5 text-emerald-400 shrink-0" />
                      <span className="font-semibold text-slate-400">Knowledge Sources:</span>
                      {item.data.sources.map((src, sIdx) => (
                        <span
                          key={sIdx}
                          className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700 text-[11px]"
                        >
                          {src.title}
                        </span>
                      ))}
                    </div>
                  )}

                  {item.data.disclaimer && (
                    <div className="flex items-start gap-1.5 text-[11px] text-amber-300/80 bg-amber-950/20 border border-amber-900/30 p-2 rounded-lg">
                      <AlertTriangle className="h-3.5 w-3.5 text-amber-400 shrink-0 mt-0.5" />
                      <span>{item.data.disclaimer}</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))
        )}

        {isAiLoading && (
          <div className="flex items-center gap-3 p-4 rounded-xl bg-slate-950 border border-slate-800 text-slate-300 max-w-sm">
            <LoadingSpinner size="sm" />
            <span className="text-xs">Analyzing portfolio & retrieving knowledge...</span>
          </div>
        )}

        {aiError && <ErrorAlert message={aiError} />}
      </div>

      {/* Input */}
      <div className="p-4 border-t border-slate-800/80 bg-slate-950/70">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          className="flex items-center gap-2"
        >
          <input
            type="text"
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            placeholder="Ask AI Advisor about your portfolio diversification or equities..."
            disabled={isAiLoading}
            className="flex-1 rounded-xl border border-slate-800 bg-slate-900 px-4 py-3 text-sm text-white placeholder-slate-500 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 disabled:opacity-60"
          />
          <button
            type="submit"
            disabled={isAiLoading || !inputMessage.trim()}
            className="px-5 py-3 rounded-xl bg-emerald-600 font-bold text-white hover:bg-emerald-500 disabled:bg-slate-800 disabled:text-slate-500 transition flex items-center justify-center gap-2 text-sm shadow-md"
            aria-label="Send message"
          >
            <Send className="h-4 w-4" />
          </button>
        </form>
      </div>
    </div>
  );
};