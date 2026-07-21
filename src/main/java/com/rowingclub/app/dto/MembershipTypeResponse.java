package com.rowingclub.app.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private Boolean allowsDailyBooking;
    private LocalDateTime createdAt;
    private List<BranchEquipmentResponse> equipment;
    private Integer totalCapacity;
}