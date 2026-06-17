package com.rowingclub.app.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Getter
public class UpdateMembershipPlanRequest {
    private String name;
    private String description;
    private Integer sessionsIncluded;
    private Integer durationDays;
    private BigDecimal price;
    private Set<UUID> membershipTypeIds;
    private Boolean isActive;
}