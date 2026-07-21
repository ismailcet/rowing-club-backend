package com.rowingclub.app.dto;

import com.rowingclub.app.entity.EquipmentType;
import lombok.Getter;

@Getter
public class BranchEquipmentRequest {

    private EquipmentType equipmentType;

    private Integer quantity;
}