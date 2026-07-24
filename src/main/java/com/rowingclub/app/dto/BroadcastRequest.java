package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastRequest {
    private String targetType;
    private String targetRole;
    private List<UUID> targetUserIds;
    private String title;
    private String message;
}