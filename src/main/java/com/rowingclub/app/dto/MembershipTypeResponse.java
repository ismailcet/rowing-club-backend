package com.rowingclub.app.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}