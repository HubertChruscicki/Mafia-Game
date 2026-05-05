import './App.css';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import PrivateRoute from './components/routing/PrivateRoute';
import PublicRoute from './components/routing/PublicRoute';
import MainLayout from './layouts/mainLayout/MainLayout';
import CreateGameRoomView from './views/createGameRoomView/CreateGameRoomView';
import DashboardView from './views/dashboardView/DashboardView';
import EnterRoomCodeView from './views/enterRoomCodeView/EnterRoomCodeView';
import LoginView from './views/loginView/LoginView';
import ProfileView from './views/profileView/ProfileView';
import RegisterView from './views/registerView/RegisterView';
import StartView from './views/startView/StartView';

function App() {
  return (
    <div className="app">
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<StartView />} />
          <Route
            path="/login"
            element={
              <PublicRoute>
                <LoginView />
              </PublicRoute>
            }
          />
          <Route
            path="/register"
            element={
              <PublicRoute>
                <RegisterView />
              </PublicRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <PrivateRoute>
                <MainLayout>
                  <DashboardView />
                </MainLayout>
              </PrivateRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <PrivateRoute>
                <MainLayout>
                  <ProfileView />
                </MainLayout>
              </PrivateRoute>
            }
          />
          <Route
            path="/create-room"
            element={
              <PrivateRoute>
                <MainLayout>
                  <CreateGameRoomView />
                </MainLayout>
              </PrivateRoute>
            }
          />
          <Route
            path="/enter-code"
            element={
              <PrivateRoute>
                <MainLayout>
                  <EnterRoomCodeView />
                </MainLayout>
              </PrivateRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
