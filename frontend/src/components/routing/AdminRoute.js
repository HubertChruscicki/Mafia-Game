import { Navigate, useLocation } from 'react-router-dom';
import { isAuthenticated } from '../../utils/auth';
import useAuthSession from '../../hooks/useAuthSession';

function AdminRoute({ children }) {
  const location = useLocation();
  const { user } = useAuthSession();

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (!user?.admin) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

export default AdminRoute;
