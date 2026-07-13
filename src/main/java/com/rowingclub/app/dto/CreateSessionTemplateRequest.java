package com.rowingclub.app.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
public class CreateSessionTemplateRequest {

    @NotNull(message = "Üyelik tipi boş olamaz")
    private UUID membershipTypeId;

    @NotBlank(message = "Şablon adı boş olamaz")
    private String name;

    @NotNull(message = "Gün boş olamaz")
    @Min(value = 1, message = "Gün 1 (Pazartesi) ile 7 (Pazar) arasında olmalıdır")
    @Max(value = 7, message = "Gün 1 (Pazartesi) ile 7 (Pazar) arasında olmalıdır")
    private Integer dayOfWeek;

    @NotNull(message = "Başlangıç saati boş olamaz")
    private LocalTime startTime;

    @NotNull(message = "Bitiş saati boş olamaz")
    private LocalTime endTime;

    @NotNull(message = "Kontejan boş olamaz")
    @Min(value = 1, message = "Kontejan en az 1 olmalıdır")
    private Integer capacity;

    /** Eğitim kontenjanı (opsiyonel, varsayılan 0). */
    @Min(value = 0, message = "Eğitim kontenjanı 0 veya daha fazla olmalıdır")
    private Integer trainingCapacity;
}