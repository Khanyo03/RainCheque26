import axios from 'axios';

// Use environment variable with fallback
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1';

console.log('API Base URL:', API_BASE_URL); // For debugging

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds timeout
});

// Request interceptor to add token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    console.log('Making request to:', config.baseURL + config.url); // Debug log
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => {
    console.log('Response received:', response.status); // Debug log
    return response;
  },
  (error) => {
    console.error('API Error:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
      url: error.config?.url
    });

    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth endpoints
export const authService = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (userData) => api.post('/auth/register', userData),
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }
};

// Event endpoints
export const eventService = {
  getAllEvents: (params = {}) => api.get('/events', { params }),
  getEventById: (id) => api.get(`/events/${id}`),
  getUpcomingEvents: (limit = 6) => api.get(`/events/upcoming?limit=${limit}`),
  searchEvents: (query) => api.get(`/events/search?q=${query}`)
};

// Ticket endpoints
export const ticketService = {
  getUserTickets: () => api.get('/tickets'),
  getTicket: (id) => api.get(`/tickets/${id}`),
  downloadTicket: (id) => api.get(`/tickets/${id}/download`, { 
    responseType: 'blob' 
  })
};

export default api;