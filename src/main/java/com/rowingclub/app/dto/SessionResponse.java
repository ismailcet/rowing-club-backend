package com.rowingclub.app.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private UUID id;
    private String templateName;
    private UUID membershipTypeId;
    private String membershipTypeName;
    private LocalDate sessionDate;
    private String dayOfWeekLabel;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer currentCapacity;
    private Integer maxCapacity;
    private Integer remainingCapacity;
    private Boolean isFull;

    // Eğitim kontenjanı (eğitim paketi sahipleri için)
    private Integer currentTrainingCapacity;
    private Integer trainingCapacity;

    private String status;

    private Boolean isEnrolled;

    private Boolean reservable;
}