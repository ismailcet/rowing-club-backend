package com.rowingclub.app.repository;

import com.rowingclub.app.entity.SessionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface SessionTemplateRepository extends JpaRepository<SessionTemplate, UUID> {
    List<SessionTemplate> findAllByIsActiveTrue();
    boolean existsByMembershipTypeIdAndDayOfWeekAndStartTimeAndIsActiveTrue(
            UUID membershipTypeId,
            Integer dayOfWeek,
            LocalTime startTime
    );
}