import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './context/AuthContext'
import './index.css'

async function enableMocking() {
  // MSW mocks have been removed from the codebase.
  // This function is retained for future use if mocks are re-added.
  if (import.meta.env.PROD) {
    return
  }

  if (import.meta.env.VITE_ENABLE_MOCKS !== 'true') {
    return
  }

  // Mocks directory has been removed; log warning if VITE_ENABLE_MOCKS is set
  console.warn('VITE_ENABLE_MOCKS is set to true but no mocks directory exists.')
}

enableMocking().then(() => {
  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </React.StrictMode>
  )
})