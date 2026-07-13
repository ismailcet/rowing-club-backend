package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class YearlySummaryResponse {
    private int year;
    private List<MonthlySummaryResponse> months; // 12 ay, Ocak..Aralık
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal net;
}