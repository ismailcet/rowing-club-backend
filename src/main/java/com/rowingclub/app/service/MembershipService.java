package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.DuplicateResourceException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.*;
import com.rowingclub.app.entity.*;
import com.rowingclub.app.repository.MembershipPlanRepository;
import com.rowingclub.app.repository.MembershipRepository;
import com.rowingclub.app.repository.MembershipTypeRepository;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipTypeRepository membershipTypeRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public MembershipPlanResponse createPlan(CreateMembershipPlanRequest request) {

        List<MembershipPlan> similarPlans = membershipPlanRepository
                .findByPriceAndDurationDaysAndIsActiveTrue(
                        request.getPrice(),
                        request.getDurationDays());

        for (MembershipPlan existingPlan : similarPlans) {
            Set<UUID> existingTypeIds = existingPlan.getPlanTypes().stream()
                    .map(pt -> pt.getMembershipType().getId())
                    .collect(Collectors.toSet());

            if (existingTypeIds.equals(request.getMembershipTypeIds())) {
                throw new DuplicateResourceException(
                        "Aynı fiyat, süre ve üyelik tiplerine sahip aktif bir plan zaten mevcut: "
                                + existingPlan.getName()
                );
            }
        }

        MembershipPlan plan = MembershipPlan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sessionsIncluded(request.getSessionsIncluded())
                .durationDays(request.getDurationDays())
                .price(request.getPrice())
                .isTraining(Boolean.TRUE.equals(request.getIsTraining()))
                .build();

        for (UUID typeId : request.getMembershipTypeIds()) {
            MembershipType membershipType = membershipTypeRepository.findById(typeId)
                    .orElseThrow(() -> new ResourceNotFoundException("MembershipType", "id", typeId));

            MembershipPlanType planType = MembershipPlanType.builder()
                    .id(new MembershipPlanType.MembershipPlanTypeId(null, typeId))
                    .plan(plan)
                    .membershipType(membershipType)
                    .build();

            plan.getPlanTypes().add(planType);
        }

        membershipPlanRepository.save(plan);
        return toMembershipPlanResponse(plan);
    }

    public List<MembershipPlanResponse> getAllActivePlans() {
        return membershipPlanRepository.findAllByIsActiveTrue()
                .stream()
                .map(this::toMembershipPlanResponse)
                .collect(Collectors.toList());
    }

    /** Üyenin satın alabileceği planlar: aktif + eğitim olmayan. */
    public List<MembershipPlanResponse> getPurchasablePlans() {
        return membershipPlanRepository.findAllByIsActiveTrue()
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsTraining()))
                .map(this::toMembershipPlanResponse)
                .collect(Collectors.toList());
    }

    public List<MembershipPlanResponse> getAllPlans() {
        return membershipPlanRepository.findAll()
                .stream()
                .map(this::toMembershipPlanResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivatePlan(UUID planId) {
        var plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipPlan", "id", planId));
        plan.setIsActive(false);
        membershipPlanRepository.save(plan);
    }

    @Transactional
    public MembershipPlanResponse updatePlan(UUID planId, UpdateMembershipPlanRequest request) {
        var plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipPlan", "id", planId));
        BigDecimal oldPrice = plan.getPrice();

        // Duplicate kontrolü (sadece ilgili alanlar değiştiyse)
        if (request.getPrice() != null || request.getDurationDays() != null || request.getMembershipTypeIds() != null) {
            BigDecimal priceToCheck = request.getPrice() != null ? request.getPrice() : plan.getPrice();
            Integer durationToCheck = request.getDurationDays() != null ? request.getDurationDays() : plan.getDurationDays();
            Set<UUID> typesToCheck = request.getMembershipTypeIds() != null
                    ? request.getMembershipTypeIds()
                    : plan.getPlanTypes().stream().map(pt -> pt.getMembershipType().getId()).collect(Collectors.toSet());

            List<MembershipPlan> similarPlans = membershipPlanRepository
                    .findByPriceAndDurationDaysAndIsActiveTrue(priceToCheck, durationToCheck);

            for (MembershipPlan existing : similarPlans) {
                if (existing.getId().equals(planId)) continue;
                Set<UUID> existingTypeIds = existing.getPlanTypes().stream()
                        .map(pt -> pt.getMembershipType().getId())
                        .collect(Collectors.toSet());
                if (existingTypeIds.equals(typesToCheck)) {
                    throw new DuplicateResourceException(
                            "Aynı fiyat, süre ve üyelik tiplerine sahip aktif bir plan zaten mevcut: " + existing.getName()
                    );
                }
            }
        }

        if (request.getName() != null) plan.setName(request.getName());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getSessionsIncluded() != null) plan.setSessionsIncluded(request.getSessionsIncluded());
        if (request.getDurationDays() != null) plan.setDurationDays(request.getDurationDays());
        if (request.getPrice() != null) plan.setPrice(request.getPrice());
        if (request.getIsActive() != null) plan.setIsActive(request.getIsActive());
        if (request.getIsTraining() != null) plan.setIsTraining(request.getIsTraining());

        if (request.getMembershipTypeIds() != null && !request.getMembershipTypeIds().isEmpty()) {
            plan.getPlanTypes().clear();
            for (UUID typeId : request.getMembershipTypeIds()) {
                var membershipType = membershipTypeRepository.findById(typeId)
                        .orElseThrow(() -> new ResourceNotFoundException("MembershipType", "id", typeId));
                var planType = MembershipPlanType.builder()
                        .id(new MembershipPlanType.MembershipPlanTypeId(planId, typeId))
                        .plan(plan)
                        .membershipType(membershipType)
                        .build();
                plan.getPlanTypes().add(planType);
            }
        }

        membershipPlanRepository.save(plan);

        if (request.getPrice() != null && oldPrice.compareTo(request.getPrice()) != 0) {
            BigDecimal newPrice = request.getPrice();
            Set<UUID> notifiedUserIds = new HashSet<>();

            membershipRepository.findAllByPlanIdAndStatus(planId, Membership.MembershipStatus.ACTIVE)
                    .forEach(m -> {
                        if (notifiedUserIds.add(m.getUser().getId())) {
                            notificationService.sendPlanPriceUpdated(m.getUser(), plan, oldPrice, newPrice);
                        }
                    });

            List<UUID> branchIds = plan.getPlanTypes().stream()
                    .map(pt -> pt.getMembershipType().getId())
                    .collect(Collectors.toList());
            if (!branchIds.isEmpty()) {
                membershipRepository.findActiveByPlanMembershipTypeIds(branchIds)
                        .forEach(m -> {
                            if (notifiedUserIds.add(m.getUser().getId())) {
                                notificationService.sendPlanPriceUpdated(m.getUser(), plan, oldPrice, newPrice);
                            }
                        });
            }
        }

        return toMembershipPlanResponse(plan);
    }

    @Transactional
    public MembershipResponse assignMembership(AssignMembershipRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        MembershipPlan plan = membershipPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("MembershipPlan", "id", request.getPlanId()));

        boolean assigningTraining = Boolean.TRUE.equals(plan.getIsTraining());

        if (assigningTraining) {
            // Eğitim paketi ömür boyu yalnızca bir kez (aktif ya da geçmiş).
            boolean hadTraining = membershipRepository.findAllByUserId(user.getId()).stream()
                    .anyMatch(m -> Boolean.TRUE.equals(m.getPlan().getIsTraining()));
            if (hadTraining) {
                throw new BusinessException(
                        "Bu üyeye eğitim paketi yalnızca bir kez verilebilir",
                        HttpStatus.CONFLICT
                );
            }
        } else {
            // Aynı branşta zaten aktif (normal) üyelik varsa engelle.
            // Eğitim üyeliği ayrı bir kova olduğundan çakışma sayılmaz.
            List<Membership> activeNormal = membershipRepository
                    .findAllByUserIdAndStatus(user.getId(), Membership.MembershipStatus.ACTIVE)
                    .stream()
                    .filter(m -> !Boolean.TRUE.equals(m.getPlan().getIsTraining()))
                    .collect(Collectors.toList());

            for (MembershipPlanType planType : plan.getPlanTypes()) {
                UUID typeId = planType.getMembershipType().getId();
                boolean conflict = activeNormal.stream().anyMatch(m ->
                        m.getPlan().getPlanTypes().stream()
                                .anyMatch(pt -> pt.getMembershipType().getId().equals(typeId)));
                if (conflict) {
                    throw new BusinessException(
                            planType.getMembershipType().getName()
                                    + " tipinde zaten aktif bir üyeliğiniz var",
                            HttpStatus.CONFLICT
                    );
                }
            }
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(plan.getDurationDays());

        var membership = Membership.builder()
                .user(user)
                .plan(plan)
                .sessionsRemaining(plan.getSessionsIncluded())
                .startDate(startDate)
                .endDate(endDate)
                .status(Membership.MembershipStatus.ACTIVE)
                .build();

        membershipRepository.save(membership);
        return toMembershipResponse(membership);
    }

    public List<MembershipResponse> getUserMemberships(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return membershipRepository
                .findAllByUserId(userId)
                .stream()
                .map(this::toMembershipResponse)
                .collect(Collectors.toList());
    }

    private MembershipPlanResponse toMembershipPlanResponse(MembershipPlan plan) {
        Set<String> typeNames = plan.getPlanTypes().stream()
                .map(pt -> pt.getMembershipType().getName())
                .collect(Collectors.toSet());

        return MembershipPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .sessionsIncluded(plan.getSessionsIncluded())
                .durationDays(plan.getDurationDays())
                .price(plan.getPrice())
                .isActive(plan.getIsActive())
                .isTraining(plan.getIsTraining())
                .membershipTypeNames(typeNames)
                .build();
    }

    private List<MembershipResponse> toMembershipResponse(List<Membership> memberships) {
        return memberships.stream()
                .map(this::toMembershipResponse)
                .collect(Collectors.toList());
    }

    private MembershipResponse toMembershipResponse(Membership membership) {
        return MembershipResponse.builder()
                .id(membership.getId())
                .userId(membership.getUser().getId())
                .userFullName(membership.getUser().getFullName())
                .plan(toMembershipPlanResponse(membership.getPlan()))
                .sessionsRemaining(membership.getSessionsRemaining())
                .startDate(membership.getStartDate())
                .endDate(membership.getEndDate())
                .status(membership.getStatus().name())
                .build();
    }

    public List<MembershipResponse> getAllMemberships() {
        return toMembershipResponse(membershipRepository.findAll());
    }
}