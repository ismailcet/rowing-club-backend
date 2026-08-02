package com.rowingclub.app.dto;

public record SettingsResponse(
        int cancellationDeadlineHours,
        String weeklySessionsTime
) {}