package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    boolean existsByCreatedById(UUID userId);

    boolean existsByTrainerId(UUID trainerId);

    @Query("""
        SELECT e FROM Expense e
        LEFT JOIN FETCH e.branchType
        LEFT JOIN FETCH e.trainer
        JOIN FETCH e.createdBy
        WHERE e.expenseDate BETWEEN :startDate AND :endDate
        ORDER BY e.expenseDate DESC, e.createdAt DESC
    """)
    List<Expense> findAllInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT e FROM Expense e
        LEFT JOIN FETCH e.branchType
        JOIN FETCH e.createdBy
        WHERE e.trainer.id = :trainerId
        ORDER BY e.expenseDate DESC, e.createdAt DESC
    """)
    List<Expense> findAllByTrainerId(@Param("trainerId") UUID trainerId);

    @Query("""
        SELECT e FROM Expense e
        LEFT JOIN FETCH e.branchType
        JOIN FETCH e.createdBy
        WHERE e.trainer.id = :trainerId
        AND e.expenseDate BETWEEN :startDate AND :endDate
        ORDER BY e.expenseDate DESC, e.createdAt DESC
    """)
    List<Expense> findAllByTrainerIdInRange(
            @Param("trainerId") UUID trainerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}