package com.rowingclub.app.dto;

import com.rowingclub.app.entity.EquipmentType;
import lombok.Getter;

@Getter
public class EquipmentLineRequest {

    private EquipmentType equipmentType;

    private Integer quantity;
}