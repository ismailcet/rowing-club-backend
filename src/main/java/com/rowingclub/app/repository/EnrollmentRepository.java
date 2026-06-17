package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Enrollment;
import com.rowingclub.app.entity.Enrollment.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByUserIdAndSessionIdAndStatus(UUID userId, UUID sessionId, EnrollmentStatus status);

    Optional<Enrollment> findByIdAndUserId(UUID id, UUID userId);

    List<Enrollment> findAllByUserIdAndStatus(UUID userId, EnrollmentStatus status);

    @Query("""
        SELECT e FROM Enrollment e
        WHERE e.session.id = :sessionId
        AND e.status = 'ACTIVE'
    """)
    List<Enrollment> findActiveEnrollmentsBySessionId(@Param("sessionId") UUID sessionId);

    List<Enrollment> findByMembershipId(UUID membershipId);

}