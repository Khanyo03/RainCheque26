import { http, HttpResponse } from 'msw'

export const handlers = [
  // Mock GET /api/v1/events
  http.get('http://localhost:8081/api/v1/events', () => {
    return HttpResponse.json({
      content: [
        {
          id: 1,
          title: "Summer Music Festival",
          description: "A day of amazing live music",
          venueName: "Johannesburg",
          venueCity: "Soccer City (FNB)",
          startTime: "2026-07-15T14:00:00",
          minPrice: 79.99,
          maxPrice: 299.99,
          status: "PUBLISHED"
        },
        {
          id: 2,
          title: "Student Night",
          description: "Tranding Music",
          venueName: "Drama",
          venueCity: "Braam",
          startTime: "2026-06-25T20:00:00",
          minPrice: 80.00,
          maxPrice: 150.00,
          status: "PUBLISHED"
        }
      ]
    })
  }),

  // Mock POST /api/v1/auth/login
  http.post('http://localhost:8081/api/v1/auth/login', async ({ request }) => {
    const { email, password } = await request.json()
    
    if (email === 'user@example.com' && password === 'Password123!') {
      return HttpResponse.json({
        token: "mock-jwt-token-12345",
        refreshToken: "mock-refresh-token-12345",
        email: "user@example.com",
        fullName: "John Doe",
        role: "USER"
      })
    }
    
    return new HttpResponse(
      JSON.stringify({ error: "Invalid credentials" }),
      { status: 401 }
    )
  })
]