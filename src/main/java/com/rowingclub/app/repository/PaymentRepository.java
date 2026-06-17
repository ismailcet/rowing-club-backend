package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByStatus(Payment.PaymentStatus status);

    List<Payment> findAllByUserId(UUID userId);
}