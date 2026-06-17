package com.rowingclub.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CreateMembershipTypeRequest {

    @NotBlank(message = "Tip adı boş olamaz")
    private String name;

    private String description;
}