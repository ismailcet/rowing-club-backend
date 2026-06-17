package com.rowingclub.app.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipResponse {
    private UUID id;
    private UUID userId;
    private String userFullName;
    private MembershipPlanResponse plan;
    private Integer sessionsRemaining;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}