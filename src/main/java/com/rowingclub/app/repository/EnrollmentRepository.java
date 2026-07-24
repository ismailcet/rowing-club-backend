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

    void deleteByUserId(UUID userId);

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

    /** Bir üyeliğin belirli tarih aralığındaki aktif rezervasyon sayısı (haftalık/günlük limit kontrolü için). */
    @Query("""
        SELECT COUNT(e) FROM Enrollment e
        WHERE e.membership.id = :membershipId
        AND e.status = 'ACTIVE'
        AND e.session.sessionDate BETWEEN :startDate AND :endDate
    """)
    long countActiveByMembershipAndDateRange(
            @Param("membershipId") UUID membershipId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    /** Üyenin branş (üyelik tipi) bazlı katıldığı (yoklaması işaretli) ders sayıları: [typeId, typeName, count]. */
    @Query("""
        SELECT mt.id, mt.name, COUNT(e)
        FROM Enrollment e
        JOIN e.session s
        JOIN s.template t
        JOIN t.membershipType mt
        WHERE e.user.id = :userId
        AND e.status = 'ACTIVE'
        AND e.isAttended = true
        GROUP BY mt.id, mt.name
    """)
    List<Object[]> countAttendedByBranch(@Param("userId") UUID userId);

    /** Üyenin tek bir branştaki katıldığı ders sayısı (seans detayında seviye göstermek için). */
    @Query("""
        SELECT COUNT(e)
        FROM Enrollment e
        JOIN e.session s
        JOIN s.template t
        WHERE e.user.id = :userId
        AND t.membershipType.id = :membershipTypeId
        AND e.status = 'ACTIVE'
        AND e.isAttended = true
    """)
    long countAttendedByUserAndBranch(@Param("userId") UUID userId,
                                      @Param("membershipTypeId") UUID membershipTypeId);

    /** Üyenin tek bir branşta eğitim kontenjanından katıldığı ders sayısı ("Eğitim N" için). */
    @Query("""
        SELECT COUNT(e)
        FROM Enrollment e
        JOIN e.session s
        JOIN s.template t
        WHERE e.user.id = :userId
        AND t.membershipType.id = :membershipTypeId
        AND e.status = 'ACTIVE'
        AND e.isAttended = true
        AND e.usedTrainingSlot = true
    """)
    long countAttendedTrainingByBranch(@Param("userId") UUID userId,
                                       @Param("membershipTypeId") UUID membershipTypeId);

}