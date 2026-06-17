package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Membership;
import com.rowingclub.app.entity.Membership.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findAllByUserIdAndStatus(UUID userId, MembershipStatus status);
    List<Membership> findAllByUserId(UUID userId);


    @Query("""
    SELECT m FROM Membership m
    WHERE m.status = 'ACTIVE'
    AND m.endDate = :targetDate
    AND m.notified7Days = false
""")
    List<Membership> findMembershipsToNotify7Days(@Param("targetDate") LocalDate targetDate);

    @Query("""
    SELECT m FROM Membership m
    WHERE m.status = 'ACTIVE'
    AND m.endDate = :targetDate
    AND m.notified3Days = false
""")
    List<Membership> findMembershipsToNotify3Days(@Param("targetDate") LocalDate targetDate);

    @Query("""
    SELECT m FROM Membership m
    WHERE m.status = 'ACTIVE'
    AND m.endDate = :targetDate
    AND m.notified1Day = false
""")
    List<Membership> findMembershipsToNotify1Day(@Param("targetDate") LocalDate targetDate);

    @Query("""
    SELECT m FROM Membership m
    WHERE m.status = 'ACTIVE'
    AND m.endDate < :today
""")
    List<Membership> findExpiredMemberships(@Param("today") LocalDate today);


    @Query("""
    SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
    FROM Membership m
    JOIN m.plan p
    JOIN p.planTypes pt
    WHERE m.user.id = :userId
    AND pt.membershipType.id = :membershipTypeId
    AND m.status IN ('ACTIVE', 'PENDING_APPROVAL')
""")
    boolean existsActiveMembershipForType(
            @Param("userId") UUID userId,
            @Param("membershipTypeId") UUID membershipTypeId
    );
}