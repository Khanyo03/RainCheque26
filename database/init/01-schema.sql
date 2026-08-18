-- H2 compatible schema - Fixed version
-- Remove the CREATE ALIAS line - it's causing duplication

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone_number VARCHAR(20),
    role VARCHAR(50) DEFAULT 'USER',
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    last_login TIMESTAMP
);

-- Venues table
CREATE TABLE IF NOT EXISTS venues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);

-- Events table
CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    venue_id BIGINT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    category VARCHAR(100),
    banner_image_url TEXT,
    thumbnail_url TEXT,
    status VARCHAR(50) DEFAULT 'DRAFT',
    metadata JSON,
    is_featured BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (venue_id) REFERENCES venues(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Ticket tiers table
CREATE TABLE IF NOT EXISTS ticket_tiers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    total_quantity INTEGER NOT NULL,
    remaining_quantity INTEGER NOT NULL,
    max_per_user INTEGER DEFAULT 10,
    min_per_user INTEGER DEFAULT 1,
    benefits JSON,
    is_active BOOLEAN DEFAULT TRUE,
    sales_start TIMESTAMP,
    sales_end TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    ticket_tier_id BIGINT NOT NULL,
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    status VARCHAR(50) DEFAULT 'PENDING',
    quantity INTEGER NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_intent_id VARCHAR(255) UNIQUE,
    seat_numbers JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (event_id) REFERENCES events(id),
    FOREIGN KEY (ticket_tier_id) REFERENCES ticket_tiers(id)
);

-- Tickets table
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    ticket_tier_id BIGINT NOT NULL,
    ticket_code VARCHAR(50) UNIQUE NOT NULL,
    qr_code TEXT,
    qr_secret VARCHAR(255),
    seat_number VARCHAR(50),
    price_paid DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    used_at TIMESTAMP,
    validated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (ticket_tier_id) REFERENCES ticket_tiers(id),
    FOREIGN KEY (validated_by) REFERENCES users(id)
);

-- Ticket batch jobs table
CREATE TABLE IF NOT EXISTS ticket_batch_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(255) NOT NULL,
    event_id BIGINT,
    total_tickets INTEGER NOT NULL,
    processed_tickets INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    error_log TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Ticket validations table
CREATE TABLE IF NOT EXISTS ticket_validations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    validated_by BIGINT NOT NULL,
    validation_method VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (validated_by) REFERENCES users(id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_events_start_time ON events(start_time);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_ticket_tiers_event ON ticket_tiers(event_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_event ON bookings(event_id);
CREATE INDEX IF NOT EXISTS idx_tickets_booking ON tickets(booking_id);
CREATE INDEX IF NOT EXISTS idx_tickets_code ON tickets(ticket_code);


-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(255) UNIQUE,
    provider_status VARCHAR(50),
    provider_transaction_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    paid_at TIMESTAMP,
    refunded_at TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_provider_ref ON payments(provider_reference);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
