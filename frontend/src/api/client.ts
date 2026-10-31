import { ApiErrorResponse } from '../types';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export class ApiException extends Error {
  public status: number;
  public errorResponse: ApiErrorResponse;

  constructor(errorResponse: ApiErrorResponse) {
    super(errorResponse.message || 'An unexpected API error occurred');
    this.name = 'ApiException';
    this.status = errorResponse.status;
    this.errorResponse = errorResponse;
  }
}

export async function request<T>(
  endpoint: string,
  options: RequestInit = {},
  retries = 1
): Promise<T> {
  const token = localStorage.getItem('stockvision_token');
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;

  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
    });
  } catch {
    if (retries > 0) {
      await new Promise((resolve) => setTimeout(resolve, 1500));
      return request<T>(endpoint, options, retries - 1);
    }
    throw new ApiException({
      status: 0,
      error: 'Network Error',
      message: 'Server is waking up (can take up to 1–2 minutes). Please retry shortly.',
    });
  }

  if (response.status === 401) {
    localStorage.removeItem('stockvision_token');
    window.dispatchEvent(new Event('auth:unauthorized'));
  }

  if (!response.ok) {
    let errorData: ApiErrorResponse;
    try {
      errorData = await response.json();
    } catch {
      errorData = {
        status: response.status,
        error: response.statusText || 'Error',
        message: `HTTP request failed with status ${response.status}`,
      };
    }
    throw new ApiException(errorData);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json() as Promise<T>;
}