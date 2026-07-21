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
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String userType;
    private Boolean isActive;
    private Boolean canViewRoster;
    private Boolean canManageAttendance;
    private Boolean canViewAthletes;
    private Boolean canManageDailyBookings;
    private List<UUID> assignedBranchIds;
    private LocalDateTime createdAt;
}