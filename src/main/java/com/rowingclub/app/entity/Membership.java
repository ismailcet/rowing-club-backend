package com.rowingclub.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    @Column(name = "sessions_remaining", nullable = false)
    private Integer sessionsRemaining;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "notified_7_days", nullable = false)
    @Builder.Default
    private Boolean notified7Days = false;

    @Column(name = "notified_3_days", nullable = false)
    @Builder.Default
    private Boolean notified3Days = false;

    @Column(name = "notified_1_day", nullable = false)
    @Builder.Default
    private Boolean notified1Day = false;

    public enum MembershipStatus {
        PENDING_APPROVAL, ACTIVE, EXPIRED, CANCELLED
    }


}