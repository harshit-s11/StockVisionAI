import React from 'react';
import { AlertCircle, X } from 'lucide-react';

export const ErrorAlert: React.FC<{
  message: string;
  onDismiss?: () => void;
}> = ({ message, onDismiss }) => {
  return (
    <div className="rounded-lg border border-rose-900/60 bg-rose-950/40 p-4 text-sm text-rose-200 flex items-start justify-between gap-3 shadow-sm">
      <div className="flex items-center gap-2.5">
        <AlertCircle className="h-5 w-5 text-rose-400 shrink-0" />
        <span>{message}</span>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          className="text-rose-400 hover:text-rose-200 rounded p-0.5 hover:bg-rose-900/40 transition"
          aria-label="Dismiss error"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
};