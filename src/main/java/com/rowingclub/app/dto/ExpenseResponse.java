package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ExpenseResponse {
    private UUID id;
    private String category;
    private UUID branchTypeId;
    private String branchTypeName;
    private UUID trainerId;
    private String trainerName;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String createdByName;
    private LocalDateTime createdAt;
}