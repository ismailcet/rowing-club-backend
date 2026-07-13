package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String title;
    private String body;
    private Boolean isRead;
    private LocalDateTime createdAt;
}