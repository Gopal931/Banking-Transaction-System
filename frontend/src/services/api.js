import axios from 'axios';

/**
 * Central Axios instance for the Banking Transaction Processing System frontend.
 * Preconfigured with the backend base URL (http://localhost:8080).
 * Note: Authentication tokens are not attached in Phase 1.
 */
const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Health check helper calling GET /api/health.
 */
export const getHealthStatus = async () => {
  const response = await api.get('/api/health');
  return response.data;
};

export default api;
