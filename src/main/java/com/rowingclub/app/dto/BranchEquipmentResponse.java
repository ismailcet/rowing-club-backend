package com.rowingclub.app.dto;

import com.rowingclub.app.entity.EquipmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchEquipmentResponse {

    private EquipmentType equipmentType;

    private Integer quantity;

    private Integer capacityPerUnit;

    private Integer totalCapacity;
}