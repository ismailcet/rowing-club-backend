package com.rowingclub.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CashPaymentRequest {

    @NotNull(message = "Plan ID boş olamaz")
    private UUID planId;
}