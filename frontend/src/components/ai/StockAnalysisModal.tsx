import React from 'react';
import { X, Sparkles, AlertCircle, BookOpen, CheckCircle2 } from 'lucide-react';
import { StockAnalysisResponse } from '../../types';
import { LoadingSpinner } from '../common/LoadingSpinner';

interface StockAnalysisModalProps {
  isOpen: boolean;
  symbol: string;
  analysisData: StockAnalysisResponse | null;
  isLoading: boolean;
  onClose: () => void;
}

export const StockAnalysisModal: React.FC<StockAnalysisModalProps> = ({
  isOpen,
  symbol,
  analysisData,
  isLoading,
  onClose,
}) => {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
      aria-labelledby="analysis-dialog-title"
    >
      <div className="relative w-full max-w-2xl rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl space-y-6 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-4">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h2 id="analysis-dialog-title" className="text-lg font-bold text-white flex items-center gap-2">
                <span>AI Stock Intelligence</span>
                <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                  {symbol}
                </span>
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">Automated fundamental & risk assessment powered by Gemini</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 transition"
            aria-label="Close dialog"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {isLoading ? (
          <div className="py-16">
            <LoadingSpinner size="lg" message={`Generating comprehensive AI analysis for ${symbol}...`} />
          </div>
        ) : analysisData ? (
          <div className="space-y-6">
            {/* Analysis Narrative */}
            <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-5 space-y-2">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">Analysis Summary</h3>
              <p className="text-sm text-slate-200 leading-relaxed whitespace-pre-line">
                {analysisData.analysis}
              </p>
            </div>

            {/* Key Factors & Risks */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="rounded-xl border border-emerald-900/40 bg-emerald-950/20 p-4 space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center gap-1.5">
                  <CheckCircle2 className="h-4 w-4" /> Key Drivers & Metrics
                </h4>
                <ul className="space-y-1 text-xs text-slate-300">
                  {analysisData.keyFactors.map((factor, idx) => (
                    <li key={idx} className="flex items-start gap-1.5">
                      <span className="text-emerald-500">•</span>
                      <span>{factor}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="rounded-xl border border-rose-900/40 bg-rose-950/20 p-4 space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-1.5">
                  <AlertCircle className="h-4 w-4" /> Risk Considerations
                </h4>
                <ul className="space-y-1 text-xs text-slate-300">
                  {analysisData.risks.map((risk, idx) => (
                    <li key={idx} className="flex items-start gap-1.5">
                      <span className="text-rose-500">•</span>
                      <span>{risk}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Sources */}
            {analysisData.sources && analysisData.sources.length > 0 && (
              <div className="flex items-center gap-2 text-xs text-slate-400 pt-2 border-t border-slate-800">
                <BookOpen className="h-4 w-4 text-emerald-400" />
                <span className="font-semibold">Grounded in Financial Docs:</span>
                <div className="flex flex-wrap gap-1.5">
                  {analysisData.sources.map((src, idx) => (
                    <span key={idx} className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[11px]">
                      {src.title}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Disclaimer */}
            <div className="rounded-lg bg-slate-950 border border-slate-800 p-3 text-[11px] text-slate-400 leading-relaxed">
              <span className="font-bold text-amber-300/80">Disclaimer: </span>
              {analysisData.disclaimer}
            </div>
          </div>
        ) : null}

        <div className="flex justify-end pt-2 border-t border-slate-800/80">
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-sm font-semibold text-white transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};