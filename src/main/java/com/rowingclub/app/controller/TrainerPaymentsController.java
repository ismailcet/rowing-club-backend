package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.ExpenseResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Antrenör · kendi ödeme geçmişi. Yalnızca giriş yapan antrenörün kendi
 * kimliğine ({@code user.getId()}) bağlıdır; başka bir antrenörün ödemesine
 * erişim yoktur (bunun için admin-only {@code AdminFinanceController} kullanılır).
 */
@RestController
@RequestMapping("/api/trainer/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANTRENÖR')")
public class TrainerPaymentsController {

    private final FinanceService financeService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getMyPayments(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ExpenseResponse> payments = (startDate != null && endDate != null)
                ? financeService.getTrainerPayments(user.getId(), startDate, endDate)
                : financeService.getTrainerPayments(user.getId());
        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}