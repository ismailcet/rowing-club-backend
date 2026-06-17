package com.rowingclub.app.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private UUID planId;
    private String planName;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String rejectionReason;
    private boolean hasReceipt;
    private LocalDateTime createdAt;
}