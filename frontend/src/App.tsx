import React, { useEffect, useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { AppRoutes } from './routes/AppRoutes';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const App: React.FC = () => {
  const [isWakingUp, setIsWakingUp] = useState<boolean>(true);

  useEffect(() => {
    let mounted = true;
    const checkHealth = async () => {
      try {
        await fetch(`${API_BASE}/health`);
      } catch {
        // Ignored; server may be spinning up or offline
      } finally {
        if (mounted) {
          setIsWakingUp(false);
        }
      }
    };
    checkHealth();
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <BrowserRouter>
      {isWakingUp && (
        <div
          role="status"
          className="bg-amber-500/10 border-b border-amber-500/20 text-amber-300 text-xs py-1.5 px-4 text-center font-medium flex items-center justify-center gap-2"
        >
          <span className="inline-block w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
          Waking up server (can take up to 1–2 minutes)...
        </div>
      )}
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
};
export default App;
