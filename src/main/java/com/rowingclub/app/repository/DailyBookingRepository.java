package com.rowingclub.app.repository;

import com.rowingclub.app.entity.DailyBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyBookingRepository extends JpaRepository<DailyBooking, UUID> {

    List<DailyBooking> findAllByBookingDateOrderByStartTime(LocalDate bookingDate);

    @Query("SELECT DISTINCT d.bookingDate FROM DailyBooking d WHERE d.bookingDate BETWEEN :startDate AND :endDate")
    List<LocalDate> findDistinctDatesInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}