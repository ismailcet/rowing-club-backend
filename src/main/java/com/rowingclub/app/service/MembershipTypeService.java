package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.DuplicateResourceException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.BranchEquipmentRequest;
import com.rowingclub.app.dto.BranchEquipmentResponse;
import com.rowingclub.app.dto.CreateMembershipTypeRequest;
import com.rowingclub.app.dto.MembershipTypeResponse;
import com.rowingclub.app.dto.UpdateMembershipTypeRequest;
import com.rowingclub.app.entity.BranchEquipment;
import com.rowingclub.app.entity.MembershipType;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.BranchEquipmentRepository;
import com.rowingclub.app.repository.MembershipPlanRepository;
import com.rowingclub.app.repository.MembershipTypeRepository;
import com.rowingclub.app.repository.TrainerBranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipTypeService {

    private final MembershipTypeRepository membershipTypeRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final BranchEquipmentRepository branchEquipmentRepository;
    private final TrainerBranchRepository trainerBranchRepository;

    @Transactional
    public MembershipTypeResponse create(CreateMembershipTypeRequest request) {
        String name = request.getName().toUpperCase();

        if (membershipTypeRepository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("Bu isimde bir üyelik tipi zaten mevcut: " + name);
        }

        var type = MembershipType.builder()
                .name(name)
                .description(request.getDescription())
                .allowsDailyBooking(Boolean.TRUE.equals(request.getAllowsDailyBooking()))
                .build();

        membershipTypeRepository.save(type);
        saveEquipment(type, request.getEquipment());
        return toResponse(type);
    }

    public List<MembershipTypeResponse> getAll() {
        return membershipTypeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Antrenör branş kısıtlaması varsa yalnızca atanan branşları döner; admin veya
     *  kısıtlaması olmayan antrenör için tüm branşlar döner. */
    public List<MembershipTypeResponse> getAllForUser(User requester) {
        List<MembershipTypeResponse> all = getAll();
        List<UUID> allowed = allowedBranchIdsOrNull(requester);
        if (allowed == null) {
            return all;
        }
        return all.stream()
                .filter(r -> allowed.contains(r.getId()))
                .collect(Collectors.toList());
    }

    /** ADMIN veya atama yapılmamış antrenör için null (kısıtlama yok) döner. */
    public List<UUID> allowedBranchIdsOrNull(User requester) {
        if (requester == null || "ADMIN".equalsIgnoreCase(requester.getUserType().getName())) {
            return null;
        }
        List<UUID> assigned = trainerBranchRepository.findMembershipTypeIdsByTrainerId(requester.getId());
        return assigned.isEmpty() ? null : assigned;
    }

    @Transactional
    public MembershipTypeResponse update(UUID id, UpdateMembershipTypeRequest request) {
        var type = membershipTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", "id", id));

        if (request.getName() != null) {
            String newName = request.getName().toUpperCase();
            membershipTypeRepository.findByName(newName).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new DuplicateResourceException("Bu isimde bir üyelik tipi zaten mevcut: " + newName);
                }
            });
            type.setName(newName);
        }

        if (request.getDescription() != null) {
            type.setDescription(request.getDescription());
        }

        if (request.getAllowsDailyBooking() != null) {
            type.setAllowsDailyBooking(request.getAllowsDailyBooking());
        }

        membershipTypeRepository.save(type);

        if (request.getEquipment() != null) {
            saveEquipment(type, request.getEquipment());
        }

        return toResponse(type);
    }

    @Transactional
    public void delete(UUID id) {
        var type = membershipTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", "id", id));

        if (membershipPlanRepository.existsActivePlanByMembershipTypeId(id)) {
            throw new BusinessException(
                    "Bu üyelik tipini kullanan aktif planlar mevcut, önce planları deaktif edin",
                    HttpStatus.CONFLICT
            );
        }

        membershipTypeRepository.delete(type);
    }

    private void saveEquipment(MembershipType type, List<BranchEquipmentRequest> equipment) {
        branchEquipmentRepository.deleteByMembershipTypeId(type.getId());

        if (equipment == null) {
            return;
        }

        for (BranchEquipmentRequest item : equipment) {
            if (item.getEquipmentType() == null) {
                continue;
            }

            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity <= 0) {
                continue;
            }

            branchEquipmentRepository.save(BranchEquipment.builder()
                    .membershipType(type)
                    .equipmentType(item.getEquipmentType())
                    .quantity(quantity)
                    .build());
        }
    }

    private MembershipTypeResponse toResponse(MembershipType type) {
        List<BranchEquipmentResponse> equipmentResponses = branchEquipmentRepository
                .findByMembershipTypeId(type.getId())
                .stream()
                .map(e -> BranchEquipmentResponse.builder()
                        .equipmentType(e.getEquipmentType())
                        .quantity(e.getQuantity())
                        .capacityPerUnit(e.getEquipmentType().getCapacityPerUnit())
                        .totalCapacity(e.getEquipmentType().getCapacityPerUnit() * e.getQuantity())
                        .build())
                .collect(Collectors.toList());

        int totalCapacity = equipmentResponses.stream()
                .mapToInt(BranchEquipmentResponse::getTotalCapacity)
                .sum();

        return MembershipTypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .description(type.getDescription())
                .allowsDailyBooking(type.getAllowsDailyBooking())
                .createdAt(type.getCreatedAt())
                .equipment(equipmentResponses)
                .totalCapacity(totalCapacity)
                .build();
    }
}