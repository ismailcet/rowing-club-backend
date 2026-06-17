package com.rowingclub.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    private String refreshToken;
}