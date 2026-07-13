package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastRequest {
    private String targetType;
    private String targetRole;
    private UUID targetUserId;
    private String title;
    private String message;
}