package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    void deleteByUserId(UUID userId);

    List<Payment> findAllByStatus(Payment.PaymentStatus status);

    List<Payment> findAllByUserId(UUID userId);

    /** Gelir tablosu: onaylanmış (SUCCESS) ödemeler, tarih aralığında. */
    @Query("""
        SELECT DISTINCT p FROM Payment p
        JOIN FETCH p.user
        JOIN FETCH p.plan pl
        LEFT JOIN FETCH pl.planTypes pt
        LEFT JOIN FETCH pt.membershipType
        WHERE p.status = 'SUCCESS'
        AND p.createdAt BETWEEN :start AND :end
        ORDER BY p.createdAt DESC
    """)
    List<Payment> findIncomeBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}