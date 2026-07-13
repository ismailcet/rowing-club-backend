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

    /** Bir şablonun bugünden itibaren planlı (gelecek) seansları. */
    @Query("""
        SELECT s FROM Session s
        WHERE s.template.id = :templateId
        AND s.status = 'SCHEDULED'
        AND s.sessionDate >= :fromDate
    """)
    List<Session> findUpcomingByTemplate(
            @Param("templateId") UUID templateId,
            @Param("fromDate") LocalDate fromDate
    );

    @Query("""
        SELECT s FROM Session s
        JOIN FETCH s.template t
        JOIN FETCH t.membershipType mt
        WHERE s.sessionDate BETWEEN :startDate AND :endDate
        AND s.status = 'SCHEDULED'
        AND mt.id IN :membershipTypeIds
        ORDER BY s.sessionDate, s.startTime
    """)
    List<Session> findByDateRangeAndMembershipTypes(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("membershipTypeIds") List<UUID> membershipTypeIds
    );

    /**
     * Üye takvim noktaları için ÇOK HAFİF sorgu: sadece dolu tarihler,
     * izinli branşlarla sınırlı. Tam seans verisi çekmekten çok daha ucuzdur.
     */
    @Query("""
        SELECT DISTINCT s.sessionDate FROM Session s
        WHERE s.sessionDate BETWEEN :startDate AND :endDate
        AND s.status = 'SCHEDULED'
        AND s.template.membershipType.id IN :membershipTypeIds
    """)
    List<LocalDate> findDistinctDatesInRangeAndMembershipTypes(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("membershipTypeIds") List<UUID> membershipTypeIds
    );

    @Query("""
        SELECT s FROM Session s
        JOIN FETCH s.template t
        JOIN FETCH t.membershipType
        WHERE s.sessionDate BETWEEN :startDate AND :endDate
        AND s.status = 'SCHEDULED'
        ORDER BY s.sessionDate, s.startTime
    """)
    List<Session> findAllByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Takvim noktaları için ÇOK HAFİF sorgu: join yok, sadece dolu tarihler.
     * Görünen ay ızgarasının (6 hafta olabilir) tamamı için kullanılır;
     * o günün TÜM seans detaylarını çekmekten çok daha ucuzdur.
     */
    @Query("""
        SELECT DISTINCT s.sessionDate FROM Session s
        WHERE s.sessionDate BETWEEN :startDate AND :endDate
        AND s.status = 'SCHEDULED'
    """)
    List<LocalDate> findDistinctDatesInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}