package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    void deleteByUserId(UUID userId);

    List<Notification> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndIsReadFalseAndIsDeletedFalse(UUID userId);

    @Modifying
    @Query("""
        UPDATE Notification n SET n.isRead = true
        WHERE n.user.id = :userId AND n.isRead = false
    """)
    int markAllReadForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteAllByCreatedAtBefore(@Param("cutoff") java.time.LocalDateTime cutoff);
}