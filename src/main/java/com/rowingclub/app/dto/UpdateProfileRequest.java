package com.rowingclub.app.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateProfileRequest {

    private String fullName;

    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    private String password;

    private String phone;
}