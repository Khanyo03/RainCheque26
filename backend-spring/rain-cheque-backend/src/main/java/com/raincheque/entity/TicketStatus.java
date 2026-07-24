package com.raincheque.entity;

public enum TicketStatus {
    ACTIVE,     // Ticket is valid and unused
    USED,       // Ticket has been validated and consumed
    CANCELLED,  // Ticket was cancelled before use
    EXPIRED     // Ticket is no longer valid due to time/event constraints
}
