package com.rowingclub.app.repository;

import com.rowingclub.app.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {
    List<MembershipPlan> findAllByIsActiveTrue();

    List<MembershipPlan> findByPriceAndDurationDaysAndIsActiveTrue(
            BigDecimal price, Integer durationDays
    );

    @Query("""
    SELECT CASE WHEN COUNT(pt) > 0 THEN true ELSE false END
    FROM MembershipPlanType pt
    WHERE pt.membershipType.id = :typeId
    AND pt.plan.isActive = true
""")
    boolean existsActivePlanByMembershipTypeId(@Param("typeId") UUID typeId);
}