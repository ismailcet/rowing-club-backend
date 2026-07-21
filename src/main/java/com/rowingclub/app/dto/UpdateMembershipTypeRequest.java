package com.rowingclub.app.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class UpdateMembershipTypeRequest {
    private String name;
    private String description;
    private Boolean allowsDailyBooking;
    private List<BranchEquipmentRequest> equipment;
}