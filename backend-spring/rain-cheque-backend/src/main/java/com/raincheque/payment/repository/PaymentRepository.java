package com.raincheque.payment.repository;

import com.raincheque.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderReference(String providerReference);

    List<Payment> findByBookingId(Long bookingId);
}
