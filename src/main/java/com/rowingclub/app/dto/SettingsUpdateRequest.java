package com.rowingclub.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SettingsUpdateRequest(
        @Min(value = 0, message = "Saat 0'dan küçük olamaz")
        @Max(value = 168, message = "Saat en fazla 168 olabilir")
        int cancellationDeadlineHours
) {}