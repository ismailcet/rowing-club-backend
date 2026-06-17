package com.rowingclub.app.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private UUID id;
    private SessionResponse session;
    private UUID membershipId;
    private String planName;
    private Integer sessionsRemaining;
    private LocalDateTime enrolledAt;
    private String status;
    private Boolean isAttended;
    private UUID userId;
    private String userFullName;
    private String userEmail;
}