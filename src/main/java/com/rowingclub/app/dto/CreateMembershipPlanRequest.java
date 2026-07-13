package com.rowingclub.app.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Getter
public class CreateMembershipPlanRequest {

    @NotBlank(message = "Plan adı boş olamaz")
    private String name;

    private String description;

    @NotNull(message = "Ders sayısı boş olamaz")
    @Min(value = 1, message = "Ders sayısı en az 1 olmalıdır")
    private Integer sessionsIncluded;

    @NotNull(message = "Süre boş olamaz")
    @Min(value = 1, message = "Süre en az 1 gün olmalıdır")
    private Integer durationDays;

    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.0", inclusive = false, message = "Fiyat 0'dan büyük olmalıdır")
    private BigDecimal price;

    @NotEmpty(message = "En az bir üyelik tipi seçilmelidir")
    private Set<UUID> membershipTypeIds;

    /** Eğitim paketi mi? (Sadece admin atar, üye satın alamaz, ömür boyu bir kez.) */
    private Boolean isTraining;
}