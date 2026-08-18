-- Insert users (let DB auto-generate IDs)
INSERT INTO users (email, password_hash, full_name, role, email_verified, created_at) VALUES
('admin@raincheque.com', '$2a$12$LGJnH.3Sz7iTQHg0FieOT.DiOyc2j/DE1fCTOVauO3AAriRJp.Ra.', 'Admin User', 'ADMIN', TRUE, CURRENT_TIMESTAMP),
('venue@example.com', '$2a$12$LGJnH.3Sz7iTQHg0FieOT.DiOyc2j/DE1fCTOVauO3AAriRJp.Ra.', 'Venue Manager', 'VENUE_ADMIN', TRUE, CURRENT_TIMESTAMP),
('user@example.com', '$2a$12$LGJnH.3Sz7iTQHg0FieOT.DiOyc2j/DE1fCTOVauO3AAriRJp.Ra.', 'John Doe', 'USER', TRUE, CURRENT_TIMESTAMP);

-- Insert venues
INSERT INTO venues (name, address, city, country, capacity, contact_email, metadata) VALUES
('Grand Theater', '123 Broadway', 'New York', 'USA', 1500, 'contact@grandtheater.com', JSON '{"parking": true, "accessible": true}'),
('City Arena', '456 Sports Blvd', 'Los Angeles', 'USA', 5000, 'info@cityarena.com', JSON '{"parking": true, "foodCourt": true}'),
('Jazz Club', '789 Music Lane', 'Chicago', 'USA', 300, 'bookings@jazzclub.com', JSON '{"ageRestriction": 21, "dinnerService": true}');

-- Insert events (venue_id must match auto-generated venue IDs)
INSERT INTO events (title, description, short_description, venue_id, start_time, end_time, category, status, is_featured, metadata) VALUES
('Summer Music Festival', 'A day of amazing live music performances featuring top artists.', 'Live music festival', 1, DATEADD('DAY', 30, CURRENT_TIMESTAMP), DATEADD('HOUR', 8, DATEADD('DAY', 30, CURRENT_TIMESTAMP)), 'MUSIC', 'PUBLISHED', TRUE, JSON '{"weatherPolicy": "standard", "parkingAvailable": true, "foodAllowed": true}'),
('Broadway: The Phantom', 'Classic musical performance of The Phantom of the Opera', 'Evening with Phantom', 2, DATEADD('DAY', 15, CURRENT_TIMESTAMP), DATEADD('HOUR', 3, DATEADD('DAY', 15, CURRENT_TIMESTAMP)), 'THEATRE', 'PUBLISHED', TRUE, JSON '{"weatherPolicy": "indoor", "parkingAvailable": true, "foodAllowed": true}'),
('Jazz Night', 'Smooth jazz evening with local artists', 'Jazz performances', 3, DATEADD('DAY', 7, CURRENT_TIMESTAMP), DATEADD('HOUR', 4, DATEADD('DAY', 7, CURRENT_TIMESTAMP)), 'MUSIC', 'PUBLISHED', FALSE, JSON '{"ageRestriction": 21, "weatherPolicy": "standard", "parkingAvailable": false, "foodAllowed": true}');

-- Insert ticket tiers (event_id must match auto-generated event IDs)
INSERT INTO ticket_tiers (event_id, name, description, price, total_quantity, remaining_quantity, max_per_user, is_active, benefits) VALUES
(1, 'VIP', 'Front row access + drinks', 299.99, 100, 100, 4, TRUE, JSON '["Front rows", "Free drinks", "Meet & greet"]'),
(1, 'General', 'Standard admission', 99.99, 500, 500, 10, TRUE, JSON '["General admission"]'),
(1, 'Early Bird', 'Limited early discount', 79.99, 200, 200, 6, TRUE, JSON '["General admission", "Early entry"]'),
(2, 'Orchestra', 'Best seats in house', 199.99, 150, 150, 6, TRUE, JSON '["Center orchestra", "Program included"]'),
(2, 'Balcony', 'Upper level seating', 89.99, 200, 200, 8, TRUE, JSON '["Balcony seating"]'),
(3, 'General', 'General admission', 45.00, 300, 300, 10, TRUE, JSON '["General admission"]');

-- Insert a sample booking (use numeric IDs, not UUIDs)
INSERT INTO bookings (user_id, event_id, ticket_tier_id, status, quantity, total_amount, booking_time) VALUES
(3, 1, 2, 'CONFIRMED', 2, 199.98, DATEADD('DAY', -1, CURRENT_TIMESTAMP));

-- Insert sample tickets (booking_id must match numeric booking ID)
INSERT INTO tickets (booking_id, ticket_tier_id, ticket_code, qr_secret, price_paid, status) VALUES
(1, 2, 'TICKET001', 'secret123', 99.99, 'ACTIVE'),
(1, 2, 'TICKET002', 'secret456', 99.99, 'ACTIVE');

