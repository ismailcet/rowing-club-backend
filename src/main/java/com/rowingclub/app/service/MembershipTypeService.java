package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.DuplicateResourceException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.CreateMembershipTypeRequest;
import com.rowingclub.app.dto.MembershipTypeResponse;
import com.rowingclub.app.dto.UpdateMembershipTypeRequest;
import com.rowingclub.app.entity.MembershipType;
import com.rowingclub.app.repository.MembershipPlanRepository;
import com.rowingclub.app.repository.MembershipTypeRepository;
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

    @Transactional
    public MembershipTypeResponse create(CreateMembershipTypeRequest request) {
        String name = request.getName().toUpperCase();

        if (membershipTypeRepository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("Bu isimde bir üyelik tipi zaten mevcut: " + name);
        }

        var type = MembershipType.builder()
                .name(name)
                .description(request.getDescription())
                .build();

        membershipTypeRepository.save(type);
        return toResponse(type);
    }

    public List<MembershipTypeResponse> getAll() {
        return membershipTypeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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

        membershipTypeRepository.save(type);
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

    private MembershipTypeResponse toResponse(MembershipType type) {
        return MembershipTypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .description(type.getDescription())
                .createdAt(type.getCreatedAt())
                .build();
    }
}