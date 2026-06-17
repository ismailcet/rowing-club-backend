package com.rowingclub.app.dto;

import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionTemplateResponse {
    private UUID id;
    private UUID membershipTypeId;
    private String name;
    private String membershipTypeName;
    private Integer dayOfWeek;
    private String dayOfWeekLabel;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Boolean isActive;
}