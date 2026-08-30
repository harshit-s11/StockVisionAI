import React from 'react';
import { AiChatPanel } from '../components/ai/AiChatPanel';
import { Sparkles, ShieldCheck } from 'lucide-react';

export const AiAdvisorPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <Sparkles className="h-6 w-6 text-emerald-400" />
            <h1 className="text-2xl font-bold text-white tracking-tight">AI Portfolio Advisor</h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Personalized, educational guidance grounded in your live portfolio metrics and pgvector knowledge base
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs font-semibold px-3 py-1.5 rounded-lg bg-emerald-950/60 text-emerald-300 border border-emerald-800/40">
          <ShieldCheck className="h-4 w-4 text-emerald-400" />
          <span>Educational & Advisory Only</span>
        </div>
      </div>

      <AiChatPanel />
    </div>
  );
};