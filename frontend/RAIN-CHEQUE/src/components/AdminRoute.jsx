import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ADMIN_ROLES = ['VENUE_ADMIN', 'ADMIN'];

function AdminRoute({ children }) {
  const { isAuthenticated, user, setRedirectPath } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    setRedirectPath(location.pathname);
    return <Navigate to="/login" replace />;
  }

  if (!user || !ADMIN_ROLES.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

export default AdminRoute;
