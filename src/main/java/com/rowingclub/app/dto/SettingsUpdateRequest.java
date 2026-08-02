package com.rowingclub.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record SettingsUpdateRequest(
        @Min(value = 0, message = "Saat 0'dan küçük olamaz")
        @Max(value = 168, message = "Saat en fazla 168 olabilir")
        Integer cancellationDeadlineHours,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                message = "Saat HH:mm formatında olmalıdır (ör. 21:30)")
        String weeklySessionsTime
) {}