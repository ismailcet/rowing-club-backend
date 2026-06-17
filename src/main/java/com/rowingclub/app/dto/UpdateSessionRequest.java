package com.rowingclub.app.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class UpdateSessionRequest {

    @Min(value = 1, message = "Kontejan en az 1 olmalıdır")
    private Integer maxCapacity;

    private String status;
}