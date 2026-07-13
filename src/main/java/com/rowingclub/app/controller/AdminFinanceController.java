package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.CreateExpenseRequest;
import com.rowingclub.app.dto.CreateIncomeRequest;
import com.rowingclub.app.dto.ExpenseResponse;
import com.rowingclub.app.dto.FinanceSummaryResponse;
import com.rowingclub.app.dto.IncomeItemResponse;
import com.rowingclub.app.dto.YearlySummaryResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Admin · Gelir-Gider. */
@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFinanceController {

    private final FinanceService financeService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<FinanceSummaryResponse>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                ApiResponse.success(financeService.getSummary(startDate, endDate)));
    }

    @GetMapping("/yearly")
    public ResponseEntity<ApiResponse<YearlySummaryResponse>> getYearlySummary(
            @RequestParam int year) {
        return ResponseEntity.ok(
                ApiResponse.success(financeService.getYearlySummary(year)));
    }

    @GetMapping("/income")
    public ResponseEntity<ApiResponse<List<IncomeItemResponse>>> getIncome(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                ApiResponse.success(financeService.getIncome(startDate, endDate)));
    }

    @PostMapping("/income")
    public ResponseEntity<ApiResponse<IncomeItemResponse>> createIncome(
            @RequestBody CreateIncomeRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Gelir kaydedildi",
                        financeService.createIncome(request, admin.getId())));
    }

    @DeleteMapping("/income/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIncome(@PathVariable UUID id) {
        financeService.deleteIncome(id);
        return ResponseEntity.ok(ApiResponse.success("Gelir silindi", null));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                ApiResponse.success(financeService.getExpenses(startDate, endDate)));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Gider kaydedildi",
                        financeService.createExpense(request, admin.getId())));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable UUID id) {
        financeService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.success("Gider silindi", null));
    }

    /** Bir antrenöre yapılmış tüm ödemeler (Gider'de category=PERSONEL kayıtları). */
    @GetMapping("/trainers/{trainerId}/payments")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getTrainerPayments(
            @PathVariable UUID trainerId) {
        return ResponseEntity.ok(
                ApiResponse.success(financeService.getTrainerPayments(trainerId)));
    }

    /** Antrenöre yeni ödeme kaydeder; otomatik olarak Gider'e (PERSONEL) düşer. */
    @PostMapping("/trainers/{trainerId}/payments")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createTrainerPayment(
            @PathVariable UUID trainerId,
            @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Ödeme kaydedildi",
                        financeService.createTrainerPayment(trainerId, request, admin.getId())));
    }
}