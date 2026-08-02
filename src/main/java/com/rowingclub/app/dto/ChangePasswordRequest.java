package com.rowingclub.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {

    @NotBlank(message = "Yeni şifre zorunludur")
    @Size(min = 6, message = "Yeni şifre en az 6 karakter olmalıdır")
    private String newPassword;
}