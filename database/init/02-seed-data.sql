-- First, clear existing users (if any)
TRUNCATE TABLE users CASCADE;

-- Insert sample venues (keep existing)
INSERT INTO venues (name, address, city, country, capacity, contact_email, metadata) VALUES
('Grand Theater', '123 Broadway', 'New York', 'USA', 1500, 'contact@grandtheater.com', '{"parking": true, "accessible": true}'),
('City Arena', '456 Sports Blvd', 'Los Angeles', 'USA', 5000, 'info@cityarena.com', '{"parking": true, "food_court": true}'),
('Jazz Club', '789 Music Lane', 'Chicago', 'USA', 300, 'bookings@jazzclub.com', '{"age_restriction": 21, "dinner_service": true}');

-- Insert users with BCRYPT hashed passwords
-- Password for all users is: Password123!
-- The hash below is for "Password123!" using BCrypt
INSERT INTO users (id, email, password_hash, full_name, role, email_verified, created_at) VALUES
(uuid_generate_v4(), 'admin@raincheque.com', '$2a$10$N.zmdr9k7uOCQbPNoKc0zvvPfzUQVf7UQgRJ1MGLYzFqjQK6QyYDi', 'Admin User', 'ADMIN', TRUE, NOW()),
(uuid_generate_v4(), 'venue@example.com', '$2a$10$N.zmdr9k7uOCQbPNoKc0zvvPfzUQVf7UQgRJ1MGLYzFqjQK6QyYDi', 'Venue Manager', 'VENUE_ADMIN', TRUE, NOW()),
(uuid_generate_v4(), 'user@example.com', '$2a$10$N.zmdr9k7uOCQbPNoKc0zvvPfzUQVf7UQgRJ1MGLYzFqjQK6QyYDi', 'John Doe', 'USER', TRUE, NOW());

-- Insert sample events
INSERT INTO events (title, description, short_description, venue_id, start_time, end_time, category, status, is_featured) VALUES
('Summer Music Festival', 'A day of amazing live music performances featuring top artists from around the world. Food trucks and drinks available.', 'Live music festival', 2, '2024-07-15 14:00:00', '2024-07-15 23:00:00', 'MUSIC', 'PUBLISHED', TRUE),
('Broadway: The Phantom', 'Classic musical performance of The Phantom of the Opera', 'Evening with Phantom', 1, '2024-06-20 19:30:00', '2024-06-20 22:30:00', 'THEATRE', 'PUBLISHED', TRUE),
('Jazz Night', 'Smooth jazz evening with local artists', 'Jazz performances', 3, '2024-06-25 20:00:00', '2024-06-25 23:00:00', 'MUSIC', 'PUBLISHED', FALSE);

-- Insert ticket tiers
INSERT INTO ticket_tiers (event_id, name, description, price, total_quantity, remaining_quantity, max_per_user, benefits) VALUES
(1, 'VIP', 'Front row access + drinks', 299.99, 100, 100, 4, '["Front rows", "Free drinks", "Meet & greet"]'),
(1, 'General', 'Standard admission', 99.99, 500, 500, 10, '["General admission"]'),
(1, 'Early Bird', 'Limited early discount', 79.99, 200, 200, 6, '["General admission", "Early entry"]'),
(2, 'Orchestra', 'Best seats in house', 199.99, 150, 150, 6, '["Center orchestra", "Program included"]'),
(2, 'Balcony', 'Upper level seating', 89.99, 200, 200, 8, '["Balcony seating"]'),
(3, 'General', 'General admission', 45.00, 300, 300, 10, '["General admission"]');