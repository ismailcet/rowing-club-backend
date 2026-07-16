package com.rowingclub.app.dto;

import lombok.Getter;

@Getter
public class UpdateMembershipTypeRequest {
    private String name;
    private String description;
    private Boolean allowsDailyBooking;
}