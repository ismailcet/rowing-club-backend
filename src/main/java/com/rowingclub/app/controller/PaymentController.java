package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.CashPaymentRequest;
import com.rowingclub.app.dto.PaymentResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/cash")
    public ResponseEntity<ApiResponse<PaymentResponse>> cashPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CashPaymentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Nakit ödeme talebiniz alındı. Admin onayı bekleniyor.",
                        paymentService.createCashPayment(user.getId(), request.getPlanId())
                ));
    }

    @PostMapping(value = "/eft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PaymentResponse>> eftPayment(
            @AuthenticationPrincipal User user,
            @RequestParam UUID planId,
            @RequestParam MultipartFile receipt) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "EFT dekontunuz alındı. Admin onayı bekleniyor.",
                        paymentService.createEftPayment(user.getId(), planId, receipt)
                ));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getMyPayments(user.getId())));
    }
}