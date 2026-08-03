import React, { Suspense, lazy } from 'react';
import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';
import ErrorBoundary from './components/ErrorBoundary';
import LoadingSpinner from './components/LoadingSpinner';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import NotFoundPage from './pages/NotFoundPage';

// Route-based code splitting: lazy-load heavier pages
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const TicketsPage = lazy(() => import('./pages/TicketsPage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage'));
const EventsPage = lazy(() => import('./pages/EventsPage'));
const EventDetailPage = lazy(() => import('./pages/EventDetailPage'));
const EventManagementPage = lazy(() => import('./pages/EventManagementPage'));
const EventFormPage = lazy(() => import('./pages/EventFormPage'));

function App() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      <main className="bg-gray-100 flex-grow">
        <div className="container mx-auto px-4 py-8">
          <ErrorBoundary>
            <Suspense fallback={<LoadingSpinner />}>
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/events" element={<EventsPage />} />
                <Route path="/events/:id" element={<EventDetailPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
                <Route path="/tickets" element={<ProtectedRoute><TicketsPage /></ProtectedRoute>} />
                <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
                <Route path="/admin/events" element={<AdminRoute><EventManagementPage /></AdminRoute>} />
                <Route path="/admin/events/new" element={<AdminRoute><EventFormPage /></AdminRoute>} />
                <Route path="/admin/events/:id/edit" element={<AdminRoute><EventFormPage /></AdminRoute>} />
                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </Suspense>
          </ErrorBoundary>
        </div>
      </main>
      <footer className="bg-black border-t mt-auto py-4">
        <div className="container mx-auto px-4 text-center text-red-600 text-sm">
          © 2026 Rain Cheque developed by Khanyo Railo. All rights reserved.
        </div>
      </footer>
    </div>
  );
}

export default App;
