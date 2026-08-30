import React from 'react';

export const LoadingSpinner: React.FC<{ size?: 'sm' | 'md' | 'lg'; message?: string }> = ({
  size = 'md',
  message,
}) => {
  const sizeClasses = {
    sm: 'h-4 w-4 border-2',
    md: 'h-8 w-8 border-3',
    lg: 'h-12 w-12 border-4',
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 gap-3">
      <div
        className={`animate-spin rounded-full border-slate-700 border-t-brand-500 ${sizeClasses[size]}`}
        role="status"
        aria-label="Loading"
      />
      {message && <span className="text-sm font-medium text-slate-400">{message}</span>}
    </div>
  );
};