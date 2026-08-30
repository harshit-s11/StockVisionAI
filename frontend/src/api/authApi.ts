import { request } from './client';
import { AuthResponse, UserResponse } from '../types';

export const authApi = {
  register: (data: { username: string; email: string; password: string }) =>
    request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  login: (data: { username: string; password: string }) =>
    request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getMe: () => request<UserResponse>('/auth/me'),
};