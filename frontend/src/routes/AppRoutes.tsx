import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from '../components/layout/AppLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { PublicRoute } from './PublicRoute';

import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { DashboardPage } from '../pages/DashboardPage';
import { MarketPage } from '../pages/MarketPage';
import { PortfolioPage } from '../pages/PortfolioPage';
import { TransactionsPage } from '../pages/TransactionsPage';
import { AiAdvisorPage } from '../pages/AiAdvisorPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Only routes */}
      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* Main Layout routes */}
      <Route element={<AppLayout />}>
        {/* Protected Dashboard and Portfolio */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/portfolio" element={<PortfolioPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/ai" element={<AiAdvisorPage />} />
        </Route>

        {/* Public Market Catalog (viewable before or after login) */}
        <Route path="/market" element={<MarketPage />} />
      </Route>

      {/* Fallback */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};