package com.raincheque.entity;

public enum BookingStatus {
    PENDING,     // booking created but not confirmed
    CONFIRMED,   // booking confirmed
    CANCELLED,   // booking cancelled
    COMPLETED    // booking completed (event attended)
}
