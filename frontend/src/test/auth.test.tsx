import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../contexts/AuthContext';
import { LoginPage } from '../pages/LoginPage';
import { authApi } from '../api/authApi';

vi.mock('../api/authApi', () => ({
  authApi: {
    login: vi.fn(),
    getMe: vi.fn().mockResolvedValue({ id: '1', username: 'demo', email: 'demo@test.com', role: 'USER' }),
  },
}));

describe('Auth Pages', () => {
  it('renders login form and handles submission', async () => {
    (authApi.login as any).mockResolvedValueOnce({
      token: 'mock.jwt.token',
      tokenType: 'Bearer',
      expiresIn: 86400,
      user: { id: '1', username: 'sarah', email: 'sarah@test.com', role: 'USER' },
    });

    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    );

    const submitBtn = screen.getByRole('button', { name: /sign in/i });
    expect(submitBtn).toBeInTheDocument();

    const usernameInput = screen.getByPlaceholderText(/sarah_connor/i);
    const passwordInput = screen.getByPlaceholderText(/••••••••/i);

    fireEvent.change(usernameInput, { target: { value: 'sarah' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        username: 'sarah',
        password: 'password123',
      });
    });
  });
});