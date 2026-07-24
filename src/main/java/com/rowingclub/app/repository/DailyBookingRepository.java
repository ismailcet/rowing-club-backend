package com.rowingclub.app.repository;

import com.rowingclub.app.entity.DailyBooking;
import com.rowingclub.app.entity.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface DailyBookingRepository extends JpaRepository<DailyBooking, UUID> {

    boolean existsByCreatedById(UUID userId);

    List<DailyBooking> findAllByBookingDateOrderByStartTime(LocalDate bookingDate);

    @Query("SELECT DISTINCT d.bookingDate FROM DailyBooking d WHERE d.bookingDate BETWEEN :startDate AND :endDate")
    List<LocalDate> findDistinctDatesInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(b.equipmentQuantity), 0) FROM DailyBooking b " +
            "WHERE b.membershipType.id = :membershipTypeId " +
            "AND b.bookingDate = :bookingDate " +
            "AND b.equipmentType = :equipmentType " +
            "AND b.startTime < :endTime AND b.endTime > :startTime")
    long sumOverlappingEquipmentQuantity(
            @Param("membershipTypeId") UUID membershipTypeId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("equipmentType") EquipmentType equipmentType,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}