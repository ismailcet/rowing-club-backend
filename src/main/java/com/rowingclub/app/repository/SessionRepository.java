package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    boolean existsByTemplateIdAndSessionDate(UUID templateId, LocalDate sessionDate);

    @Query("""
        SELECT s FROM Session s
        WHERE s.sessionDate BETWEEN :startDate AND :endDate
        AND s.status = 'SCHEDULED'
        AND s.template.membershipType.id IN :membershipTypeIds
        ORDER BY s.sessionDate, s.startTime
    """)
    List<Session> findByDateRangeAndMembershipTypes(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("membershipTypeIds") List<UUID> membershipTypeIds
    );

    @Query("""
        SELECT s FROM Session s
        WHERE s.sessionDate BETWEEN :startDate AND :endDate
        AND s.status = 'SCHEDULED'
        ORDER BY s.sessionDate, s.startTime
    """)
    List<Session> findAllByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}