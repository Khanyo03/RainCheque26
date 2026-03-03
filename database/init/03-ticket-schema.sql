-- Tickets table for individual tickets
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    booking_id UUID REFERENCES bookings(id) ON DELETE CASCADE,
    ticket_tier_id BIGINT REFERENCES ticket_tiers(id),
    ticket_code VARCHAR(50) UNIQUE NOT NULL,
    qr_code TEXT, -- Base64 encoded QR code or URL to stored image
    qr_secret VARCHAR(255), -- Secret for QR verification
    seat_number VARCHAR(50), -- Optional: specific seat assignment
    price_paid DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, USED, REFUNDED, CANCELLED
    used_at TIMESTAMP,
    validated_by UUID REFERENCES users(id), -- Staff who validated the ticket
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_ticket_status CHECK (status IN ('ACTIVE', 'USED', 'REFUNDED', 'CANCELLED'))
);

-- Batch job tracking for ticket generation
CREATE TABLE ticket_batch_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(255) NOT NULL,
    event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
    total_tickets INTEGER NOT NULL,
    processed_tickets INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    error_log TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ticket validation log
CREATE TABLE ticket_validations (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT REFERENCES tickets(id) ON DELETE CASCADE,
    validated_by UUID REFERENCES users(id),
    validation_method VARCHAR(50), -- SCANNER, MANUAL, API
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX idx_tickets_ticket_code ON tickets(ticket_code);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_event_tier ON tickets(ticket_tier_id);
CREATE INDEX idx_ticket_validations_ticket ON ticket_validations(ticket_id);
CREATE INDEX idx_ticket_batch_event ON ticket_batch_jobs(event_id);

-- Trigger for updating updated_at
CREATE TRIGGER update_tickets_updated_at BEFORE UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();