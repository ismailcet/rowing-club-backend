package com.rowingclub.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class AssignMembershipRequest {

    @NotNull(message = "Kullanıcı ID boş olamaz")
    private UUID userId;

    @NotNull(message = "Plan ID boş olamaz")
    private UUID planId;
}