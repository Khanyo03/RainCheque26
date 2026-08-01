import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function ProtectedRoute({ children }) {
  const { isAuthenticated, setRedirectPath } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    setRedirectPath(location.pathname);
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default ProtectedRoute;
