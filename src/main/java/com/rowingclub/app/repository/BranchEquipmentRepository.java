package com.rowingclub.app.repository;

import com.rowingclub.app.entity.BranchEquipment;
import com.rowingclub.app.entity.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchEquipmentRepository extends JpaRepository<BranchEquipment, UUID> {

    List<BranchEquipment> findByMembershipTypeId(UUID membershipTypeId);

    Optional<BranchEquipment> findByMembershipTypeIdAndEquipmentType(
            UUID membershipTypeId, EquipmentType equipmentType);

    void deleteByMembershipTypeId(UUID membershipTypeId);
}