package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ReminderRuleResponse {
    private UUID id;
    private String title;
    private String message;
    private String targetType;
    private String targetRole;
    private UUID targetUserId;
    private String targetUserName;
    private List<String> times;
    private List<Integer> daysOfWeek;
    private Boolean isActive;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime lastSentAt;
}