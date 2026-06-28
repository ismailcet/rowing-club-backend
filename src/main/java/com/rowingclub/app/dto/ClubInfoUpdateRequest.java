package com.rowingclub.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubInfoUpdateRequest(
        @NotBlank(message = "Kulüp adı zorunludur")
        @Size(max = 255)
        String name,

        String about,

        @Size(max = 30)
        String phone,

        @Size(max = 255)
        String email,

        String address,

        Double latitude,

        Double longitude,

        @Size(max = 255)
        String instagram,

        @Size(max = 255)
        String website
) {
}