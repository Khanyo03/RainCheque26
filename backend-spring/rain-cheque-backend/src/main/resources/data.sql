-- Insert sample users (password: Password123!)
INSERT INTO users (id, email, password_hash, full_name, role, email_verified, created_at) VALUES
('11111111-1111-1111-1111-111111111111', 'admin@raincheque.com', '$2a$10$N.zmdr9k7uOCQbPNoKc0zvvPfzUQVf7UQgRJ1MGLYzFqjQK6QyYDi', 'Admin User', 'ADMIN', TRUE, CURRENT_TIMESTAMP()),
('22222222-2222-2222-2222-222222222222', 'venue@example.com', '$2a$10$N.zmdr9k7uOCQbPNoKc0zvvPfzUQVf7UQgRJ1MGLYzFqjQK6QyYDi', 'Venue Manager', 'VENUE_ADMIN', TRUE, CURRENT_TIMESTAMP()),
('33333333-3333-3333-3333-333333333333', 'user@example.com', '$2a$10$N.zmdr9k7uOCQbPNoKc0zvvPfzUQVf7UQgRJ1MGLYzFqjQK6QyYDi', 'John Doe', 'USER', TRUE, CURRENT_TIMESTAMP());

-- Insert venues
INSERT INTO venues (name, address, city, country, capacity, contact_email, metadata) VALUES
('Grand Theater', '123 Broadway', 'New York', 'USA', 1500, 'contact@grandtheater.com', '{ "parking": true, "accessible": true }'),
('City Arena', '456 Sports Blvd', 'Los Angeles', 'USA', 5000, 'info@cityarena.com', '{ "parking": true, "food_court": true }'),
('Jazz Club', '789 Music Lane', 'Chicago', 'USA', 300, 'bookings@jazzclub.com', '{ "age_restriction": 21, "dinner_service": true }');

-- Insert events
INSERT INTO events (title, description, short_description, venue_id, start_time, end_time, category, status, is_featured, metadata) VALUES
('Summer Music Festival', 'A day of amazing live music performances featuring top artists.', 'Live music festival', 1, DATEADD('DAY', 30, CURRENT_TIMESTAMP()), DATEADD('HOUR', 8, DATEADD('DAY', 30, CURRENT_TIMESTAMP())), 'MUSIC', 'PUBLISHED', TRUE, '{ "weather_policy": "standard" }'),
('Broadway: The Phantom', 'Classic musical performance of The Phantom of the Opera', 'Evening with Phantom', 2, DATEADD('DAY', 15, CURRENT_TIMESTAMP()), DATEADD('HOUR', 3, DATEADD('DAY', 15, CURRENT_TIMESTAMP())), 'THEATRE', 'PUBLISHED', TRUE, '{ "weather_policy": "indoor" }'),
('Jazz Night', 'Smooth jazz evening with local artists', 'Jazz performances', 3, DATEADD('DAY', 7, CURRENT_TIMESTAMP()), DATEADD('HOUR', 4, DATEADD('DAY', 7, CURRENT_TIMESTAMP())), 'MUSIC', 'PUBLISHED', FALSE, '{ "weather_policy": "standard" }');

-- Insert ticket tiers
INSERT INTO ticket_tiers (event_id, name, description, price, total_quantity, remaining_quantity, max_per_user, benefits) VALUES
(1, 'VIP', 'Front row access + drinks', 299.99, 100, 100, 4, '["Front rows", "Free drinks", "Meet & greet"]'),
(1, 'General', 'Standard admission', 99.99, 500, 500, 10, '["General admission"]'),
(1, 'Early Bird', 'Limited early discount', 79.99, 200, 200, 6, '["General admission", "Early entry"]'),
(2, 'Orchestra', 'Best seats in house', 199.99, 150, 150, 6, '["Center orchestra", "Program included"]'),
(2, 'Balcony', 'Upper level seating', 89.99, 200, 200, 8, '["Balcony seating"]'),
(3, 'General', 'General admission', 45.00, 300, 300, 10, '["General admission"]');

-- Insert a sample booking
INSERT INTO bookings (id, user_id, event_id, ticket_tier_id, status, quantity, total_amount, booking_time) VALUES
('44444444-4444-4444-4444-444444444444', '33333333-3333-3333-3333-333333333333', 1, 2, 'CONFIRMED', 2, 199.98, DATEADD('DAY', -1, CURRENT_TIMESTAMP()));

-- Insert sample tickets
INSERT INTO tickets (booking_id, ticket_tier_id, ticket_code, qr_secret, price_paid, status) VALUES
('44444444-4444-4444-4444-444444444444', 2, 'TICKET001', 'secret123', 99.99, 'ACTIVE'),
('44444444-4444-4444-4444-444444444444', 2, 'TICKET002', 'secret456', 99.99, 'ACTIVE');