package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequest {
    /** SUBE, PERSONEL, SABIT_GIDER, DIGER */
    private String category;
    /** category = SUBE iken zorunlu. */
    private java.util.UUID branchTypeId;
    /** category = PERSONEL iken zorunlu (trainer payment ucunda otomatik set edilir). */
    private java.util.UUID trainerId;
    private String description;
    private BigDecimal amount;
    /** Boşsa bugün kullanılır. */
    private LocalDate expenseDate;
}