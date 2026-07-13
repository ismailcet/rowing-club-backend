package com.rowingclub.app.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer sessionsIncluded;
    private Integer durationDays;
    private BigDecimal price;
    private Boolean isActive;
    private Boolean isTraining;
    private Set<String> membershipTypeNames;
}