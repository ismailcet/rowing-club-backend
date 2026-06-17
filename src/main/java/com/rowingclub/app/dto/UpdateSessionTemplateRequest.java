package com.rowingclub.app.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
public class UpdateSessionTemplateRequest {
    private UUID membershipTypeId;
    private String name;

    @Min(value = 1, message = "Gün 1 ile 7 arasında olmalıdır")
    @Max(value = 7, message = "Gün 1 ile 7 arasında olmalıdır")
    private Integer dayOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;

    @Min(value = 1, message = "Kontejan en az 1 olmalıdır")
    private Integer capacity;

    private Boolean isActive;
}