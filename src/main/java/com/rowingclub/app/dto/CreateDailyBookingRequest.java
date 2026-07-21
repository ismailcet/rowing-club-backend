package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDailyBookingRequest {
    private UUID membershipTypeId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private List<EquipmentLineRequest> equipmentLines;
    private String customerName;
    private String customerPhone;
    private String notes;
    private Boolean paymentReceived;
}