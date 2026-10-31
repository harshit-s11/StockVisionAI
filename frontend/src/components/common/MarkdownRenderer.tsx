import React from 'react';
import ReactMarkdown from 'react-markdown';

interface MarkdownRendererProps {
  content: string;
  className?: string;
}

/**
 * Sanitizes markdown text by stripping stray SVG markup or code blocks
 * that LLMs may occasionally generate when attempting visual diagrams.
 */
function sanitizeMarkdown(text: string): string {
  if (!text) return '';
  return text
    .replace(/```svg[\s\S]*?```/gi, '')
    .replace(/<svg[\s\S]*?<\/svg>/gi, '')
    .replace(/<\/?svg[^>]*>/gi, '')
    .trim();
}

export const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({ content, className = '' }) => {
  const sanitized = sanitizeMarkdown(content);

  return (
    <div className={`markdown-content space-y-2 text-sm text-slate-200 leading-relaxed ${className}`}>
      <ReactMarkdown
        components={{
          h1: ({ children }) => (
            <h1 className="text-base font-bold text-white mt-3 mb-1.5 first:mt-0">{children}</h1>
          ),
          h2: ({ children }) => (
            <h2 className="text-sm font-bold text-white mt-2.5 mb-1 first:mt-0">{children}</h2>
          ),
          h3: ({ children }) => (
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-400 mt-2 mb-1 first:mt-0">{children}</h3>
          ),
          h4: ({ children }) => (
            <h4 className="text-xs font-semibold text-emerald-300 mt-1.5 mb-0.5 first:mt-0">{children}</h4>
          ),
          p: ({ children }) => (
            <p className="leading-relaxed mb-2 last:mb-0 text-slate-200">{children}</p>
          ),
          strong: ({ children }) => (
            <strong className="font-semibold text-white">{children}</strong>
          ),
          em: ({ children }) => (
            <em className="italic text-slate-300">{children}</em>
          ),
          ul: ({ children }) => (
            <ul className="list-disc pl-5 space-y-1 mb-2 text-slate-200">{children}</ul>
          ),
          ol: ({ children }) => (
            <ol className="list-decimal pl-5 space-y-1 mb-2 text-slate-200">{children}</ol>
          ),
          li: ({ children }) => (
            <li className="leading-relaxed">{children}</li>
          ),
          hr: () => (
            <hr className="border-slate-800 my-2.5" />
          ),
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-emerald-500/60 pl-3 italic text-slate-300 my-2">
              {children}
            </blockquote>
          ),
          code: ({ children, className }) => (
            <code className={`rounded bg-slate-800/80 px-1.5 py-0.5 text-xs font-mono text-emerald-300 ${className || ''}`}>
              {children}
            </code>
          ),
          pre: ({ children }) => (
            <pre className="rounded-lg bg-slate-900 border border-slate-800 p-3 overflow-x-auto my-2 text-xs font-mono text-slate-300">
              {children}
            </pre>
          ),
        }}
      >
        {sanitized}
      </ReactMarkdown>
    </div>
  );
};
