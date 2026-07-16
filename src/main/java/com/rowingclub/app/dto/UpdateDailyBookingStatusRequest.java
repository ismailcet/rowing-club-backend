package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDailyBookingStatusRequest {
    private Boolean paymentReceived;
    private Boolean arrived;
}