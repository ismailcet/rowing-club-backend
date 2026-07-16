package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DailyBookingResponse {
    private UUID id;
    private UUID membershipTypeId;
    private String membershipTypeName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private String customerName;
    private String customerPhone;
    private String notes;
    private Boolean paymentReceived;
    private Boolean arrived;
    private String createdByName;
    private LocalDateTime createdAt;
}