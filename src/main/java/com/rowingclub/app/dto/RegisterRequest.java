package com.rowingclub.app.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class RegisterRequest {

    @NotBlank(message = "Ad soyad boş olamaz")
    private String fullName;

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    private String password;

    private String phone;

}