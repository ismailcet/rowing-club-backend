package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IncomeRepository extends JpaRepository<Income, UUID> {

    @Query("""
        SELECT i FROM Income i
        LEFT JOIN FETCH i.branchType
        JOIN FETCH i.createdBy
        WHERE i.incomeDate BETWEEN :startDate AND :endDate
        ORDER BY i.incomeDate DESC, i.createdAt DESC
    """)
    List<Income> findAllInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}