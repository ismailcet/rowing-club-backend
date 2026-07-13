package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Yeni manuel gelir kaydı oluşturur (üyelik ödemesi dışındaki gelirler). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncomeRequest {
    /** SUBE, DIGER */
    private String category;
    /** category = SUBE iken zorunlu. */
    private UUID branchTypeId;
    private String description;
    private BigDecimal amount;
    /** Boşsa bugün kullanılır. */
    private LocalDate incomeDate;
}