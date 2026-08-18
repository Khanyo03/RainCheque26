import axios from 'axios';

// Use environment variable with fallback
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor to add token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Store reference to the logout function provided by AuthContext
let logoutHandler = null;

/**
 * Called from AuthProvider to wire up the logout function
 * so the Axios interceptor can trigger logout on 401 responses.
 */
export function setupInterceptors(logoutFn) {
  logoutHandler = logoutFn;
}

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      if (logoutHandler) {
        logoutHandler();
      } else {
        // Fallback if AuthProvider hasn't mounted yet
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
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

// Admin Event endpoints
export const adminEventService = {
  list: (params) => api.get('/admin/events', { params }),
  get: (id) => api.get(`/admin/events/${id}`),
  create: (data) => api.post('/admin/events', data),
  update: (id, data) => api.put(`/admin/events/${id}`, data),
  updateStatus: (id, status) => api.patch(`/admin/events/${id}/status`, { status }),
  delete: (id) => api.delete(`/admin/events/${id}`),
  uploadImage: (id, file, target) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('target', target);
    return api.post(`/admin/events/${id}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  }
};

// Ticket endpoints
export const ticketService = {
  getUserTickets: () => api.get('/tickets'),
  getTicket: (id) => api.get(`/tickets/${id}`),
  downloadTicket: (id) => api.get(`/tickets/${id}/download`, { 
    responseType: 'blob' 
  })
};

// Payment endpoints
export const paymentService = {
  initiateCheckout: (paymentData) => api.post('/payments/checkout', paymentData),
  getStatus: (id) => api.get(`/payments/${id}`),
  cancel: (id) => api.post(`/payments/${id}/cancel`),
};

export default api;
