package com.rowingclub.app.dto;

import java.util.UUID;

public record ClubInfoResponse(
        UUID id,
        String name,
        String about,
        String phone,
        String email,
        String address,
        Double latitude,
        Double longitude,
        String instagram,
        String website
) {
}