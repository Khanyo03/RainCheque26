import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { setupInterceptors } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });
  const navigate = useNavigate();
  const redirectPathRef = useRef(null);

  const isAuthenticated = !!token;

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  }, [navigate]);

  const login = useCallback((newToken, newUser) => {
    setToken(newToken);
    setUser(newUser);
    localStorage.setItem('token', newToken);
    localStorage.setItem('user', JSON.stringify(newUser));

    const savedPath = redirectPathRef.current || sessionStorage.getItem('redirectPath');
    sessionStorage.removeItem('redirectPath');
    redirectPathRef.current = null;
    navigate(savedPath || '/dashboard');
  }, [navigate]);

  const setRedirectPath = useCallback((path) => {
    redirectPathRef.current = path;
    sessionStorage.setItem('redirectPath', path);
  }, []);

  // Set up Axios interceptor to call logout on 401
  useEffect(() => {
    setupInterceptors(logout);
  }, [logout]);

  const value = {
    token,
    user,
    isAuthenticated,
    login,
    logout,
    setRedirectPath,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
