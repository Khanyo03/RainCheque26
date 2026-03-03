import api from './services/api';

const testBackendConnection = async () => {
  console.log('Testing backend connection...');
  
  try {
    // Test 1: Check if backend is reachable
    const healthCheck = await api.get('/events');
    console.log('✅ Backend is reachable!');
    console.log('Response:', healthCheck.data);
    
    // Test 2: Try login
    try {
      const login = await api.post('/auth/login', {
        email: 'user@example.com',
        password: 'Password123!'
      });
      console.log('✅ Login successful!');
      console.log('Token:', login.data.token);
    } catch (loginErr) {
      console.log('❌ Login failed:', loginErr.response?.data || loginErr.message);
    }
    
  } catch (err) {
    console.error('❌ Cannot connect to backend!');
    console.error('Error:', err.message);
    console.error('Make sure backend is running on http://localhost:8081');
  }
};

// Run the test
testBackendConnection();