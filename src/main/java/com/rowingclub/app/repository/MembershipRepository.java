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

    /** Üyenin sahip olduğu (üyeliklerinden gelen) farklı branşlar: [typeId, typeName]. */
    @Query("""
    SELECT DISTINCT mt.id, mt.name
    FROM Membership m
    JOIN m.plan p
    JOIN p.planTypes pt
    JOIN pt.membershipType mt
    WHERE m.user.id = :userId
""")
    List<Object[]> findMemberBranches(@Param("userId") UUID userId);


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

    /**
     * COMPLETED adayları: ACTIVE ve ders hakkı 0 olan üyelikler.
     * (Son dersin tarihi geçti mi kontrolü serviste enrollment'lara bakılarak yapılır.)
     */
    @Query("""
    SELECT m FROM Membership m
    WHERE m.status = 'ACTIVE'
    AND m.sessionsRemaining = 0
""")
    List<Membership> findActiveExhaustedMemberships();


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

    /** Haftalık program açıldı bildirimi için: en az bir aktif üyeliği olan kullanıcılar. */
    @Query("SELECT DISTINCT m.user FROM Membership m WHERE m.status = 'ACTIVE'")
    List<com.rowingclub.app.entity.User> findActiveMembershipUsers();

    List<Membership> findAllByPlanIdAndStatus(UUID planId, MembershipStatus status);

    /** Fiyatı güncellenen bir plan ile aynı branş(lar)ı paylaşan diğer aktif üyelikler. */
    @Query("""
    SELECT DISTINCT m FROM Membership m
    JOIN m.plan p
    JOIN p.planTypes pt
    WHERE pt.membershipType.id IN :membershipTypeIds
    AND m.status = 'ACTIVE'
""")
    List<Membership> findActiveByPlanMembershipTypeIds(
            @Param("membershipTypeIds") List<UUID> membershipTypeIds);
}