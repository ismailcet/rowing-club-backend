package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.dto.PaymentResponse;
import com.rowingclub.app.dto.RejectPaymentRequest;
import com.rowingclub.app.service.FileStorageService;
import com.rowingclub.app.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final FileStorageService fileStorageService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPendingPayments() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPendingPayments()));
    }

    @PutMapping("/{paymentId}/approve")
    public ResponseEntity<ApiResponse<PaymentResponse>> approve(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(
                ApiResponse.success("Ödeme onaylandı", paymentService.approve(paymentId))
        );
    }

    @PutMapping("/{paymentId}/reject")
    public ResponseEntity<ApiResponse<PaymentResponse>> reject(
            @PathVariable UUID paymentId,
            @RequestBody RejectPaymentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Ödeme reddedildi", paymentService.reject(paymentId, request))
        );
    }

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<Resource> getReceipt(@PathVariable UUID paymentId) {
        var payment = paymentService.getPaymentEntityById(paymentId);

        if (payment.getReceiptPath() == null) {
            throw new BusinessException("Bu ödemeye ait dekont bulunamadı", HttpStatus.NOT_FOUND);
        }

        Resource resource = fileStorageService.loadReceipt(payment.getReceiptPath());

        String filename = payment.getReceiptPath();
        MediaType mediaType = filename.endsWith(".pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}