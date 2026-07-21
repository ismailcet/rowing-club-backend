package com.rowingclub.app.entity;

public enum EquipmentType {

    TEKLI(1),
    IKILI(2);

    private final int capacityPerUnit;

    EquipmentType(int capacityPerUnit) {
        this.capacityPerUnit = capacityPerUnit;
    }

    public int getCapacityPerUnit() {
        return capacityPerUnit;
    }
}