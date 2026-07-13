package com.rowingclub.app.repository;

import com.rowingclub.app.entity.ReminderRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ReminderRuleRepository extends JpaRepository<ReminderRule, UUID> {

    @Query("""
        SELECT r FROM ReminderRule r
        LEFT JOIN FETCH r.targetUser
        JOIN FETCH r.createdBy
        ORDER BY r.createdAt DESC
    """)
    List<ReminderRule> findAllWithDetails();

    List<ReminderRule> findAllByIsActiveTrue();
}