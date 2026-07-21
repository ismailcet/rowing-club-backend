package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.CreateDailyBookingRequest;
import com.rowingclub.app.dto.DailyBookingResponse;
import com.rowingclub.app.dto.EquipmentLineRequest;
import com.rowingclub.app.dto.UpdateDailyBookingStatusRequest;
import com.rowingclub.app.entity.BranchEquipment;
import com.rowingclub.app.entity.DailyBooking;
import com.rowingclub.app.entity.EquipmentType;
import com.rowingclub.app.entity.MembershipType;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.BranchEquipmentRepository;
import com.rowingclub.app.repository.DailyBookingRepository;
import com.rowingclub.app.repository.MembershipTypeRepository;
import com.rowingclub.app.repository.TrainerBranchRepository;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyBookingService {

    private final DailyBookingRepository dailyBookingRepository;
    private final MembershipTypeRepository membershipTypeRepository;
    private final BranchEquipmentRepository branchEquipmentRepository;
    private final UserRepository userRepository;
    private final TrainerBranchRepository trainerBranchRepository;

    @Transactional(readOnly = true)
    public List<DailyBookingResponse> getForDate(LocalDate date, User requester) {
        List<DailyBookingResponse> all = dailyBookingRepository.findAllByBookingDateOrderByStartTime(date)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        List<UUID> allowed = allowedBranchIdsOrNull(requester);
        if (allowed == null) {
            return all;
        }
        return all.stream()
                .filter(r -> allowed.contains(r.getMembershipTypeId()))
                .collect(Collectors.toList());
    }

    /** ADMIN veya atama yapılmamış antrenör için null (kısıtlama yok) döner. */
    private List<UUID> allowedBranchIdsOrNull(User requester) {
        if (requester == null || "ADMIN".equalsIgnoreCase(requester.getUserType().getName())) {
            return null;
        }
        List<UUID> assigned = trainerBranchRepository.findMembershipTypeIdsByTrainerId(requester.getId());
        return assigned.isEmpty() ? null : assigned;
    }

    @Transactional(readOnly = true)
    public List<String> getDates(LocalDate startDate, LocalDate endDate) {
        return dailyBookingRepository.findDistinctDatesInRange(startDate, endDate)
                .stream()
                .map(LocalDate::toString)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<DailyBookingResponse> create(CreateDailyBookingRequest request, UUID creatorId) {
        MembershipType type = membershipTypeRepository.findById(request.getMembershipTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MembershipType", "id", request.getMembershipTypeId()));

        if (!Boolean.TRUE.equals(type.getAllowsDailyBooking())) {
            throw new BusinessException(
                    "Bu branş günlük rezervasyona açık değil", HttpStatus.BAD_REQUEST);
        }
        if (request.getBookingDate() == null) {
            throw new BusinessException("Tarih zorunludur", HttpStatus.BAD_REQUEST);
        }
        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "Geçmiş bir tarihe rezervasyon eklenemez", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException(
                    "Başlangıç ve bitiş saati zorunludur", HttpStatus.BAD_REQUEST);
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(
                    "Bitiş saati başlangıçtan sonra olmalı", HttpStatus.BAD_REQUEST);
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        List<DailyBooking> toSave = new ArrayList<>();

        boolean hasEquipmentLines = request.getEquipmentLines() != null
                && request.getEquipmentLines().stream()
                .anyMatch(l -> l.getEquipmentType() != null
                        && l.getQuantity() != null && l.getQuantity() > 0);

        if (hasEquipmentLines) {
            Map<EquipmentType, Integer> merged = new LinkedHashMap<>();
            for (EquipmentLineRequest line : request.getEquipmentLines()) {
                if (line.getEquipmentType() == null) {
                    continue;
                }
                int qty = line.getQuantity() == null ? 0 : line.getQuantity();
                if (qty <= 0) {
                    continue;
                }
                merged.merge(line.getEquipmentType(), qty, Integer::sum);
            }

            for (Map.Entry<EquipmentType, Integer> entry : merged.entrySet()) {
                EquipmentType eqType = entry.getKey();
                int requestedQuantity = entry.getValue();

                BranchEquipment equipment = branchEquipmentRepository
                        .findByMembershipTypeIdAndEquipmentType(type.getId(), eqType)
                        .orElseThrow(() -> new BusinessException(
                                "Bu branş için " + eqType.name() + " ekipmanı tanımlı değil",
                                HttpStatus.BAD_REQUEST));

                if (requestedQuantity > equipment.getQuantity()) {
                    throw new BusinessException(
                            "Bu branşta toplam " + equipment.getQuantity() + " adet "
                                    + eqType.name() + " ekipmanı var",
                            HttpStatus.BAD_REQUEST);
                }

                long overlappingQuantity = dailyBookingRepository.sumOverlappingEquipmentQuantity(
                        type.getId(),
                        request.getBookingDate(),
                        eqType,
                        request.getStartTime(),
                        request.getEndTime());

                if (overlappingQuantity + requestedQuantity > equipment.getQuantity()) {
                    throw new BusinessException(
                            "Bu saatte uygun ekipman kalmadı (" + overlappingQuantity + "/"
                                    + equipment.getQuantity() + " dolu, " + requestedQuantity + " adet "
                                    + eqType.name() + " istendi)",
                            HttpStatus.CONFLICT);
                }

                toSave.add(DailyBooking.builder()
                        .membershipType(type)
                        .bookingDate(request.getBookingDate())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .capacity(requestedQuantity * eqType.getCapacityPerUnit())
                        .equipmentType(eqType)
                        .equipmentQuantity(requestedQuantity)
                        .customerName(request.getCustomerName())
                        .customerPhone(request.getCustomerPhone())
                        .notes(request.getNotes())
                        .paymentReceived(Boolean.TRUE.equals(request.getPaymentReceived()))
                        .createdBy(creator)
                        .build());
            }
        } else {
            if (request.getCapacity() == null || request.getCapacity() <= 0) {
                throw new BusinessException("Kişi sayısı en az 1 olmalı", HttpStatus.BAD_REQUEST);
            }

            toSave.add(DailyBooking.builder()
                    .membershipType(type)
                    .bookingDate(request.getBookingDate())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .capacity(request.getCapacity())
                    .customerName(request.getCustomerName())
                    .customerPhone(request.getCustomerPhone())
                    .notes(request.getNotes())
                    .paymentReceived(Boolean.TRUE.equals(request.getPaymentReceived()))
                    .createdBy(creator)
                    .build());
        }

        dailyBookingRepository.saveAll(toSave);
        return toSave.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID id) {
        if (!dailyBookingRepository.existsById(id)) {
            throw new ResourceNotFoundException("DailyBooking", "id", id);
        }
        dailyBookingRepository.deleteById(id);
    }

    @Transactional
    public DailyBookingResponse updateStatus(UUID id, UpdateDailyBookingStatusRequest request) {
        DailyBooking booking = dailyBookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DailyBooking", "id", id));

        if (request.getPaymentReceived() != null) {
            booking.setPaymentReceived(request.getPaymentReceived());
        }
        if (request.getArrived() != null) {
            booking.setArrived(request.getArrived());
        }

        dailyBookingRepository.save(booking);
        return toResponse(booking);
    }

    private DailyBookingResponse toResponse(DailyBooking b) {
        return DailyBookingResponse.builder()
                .id(b.getId())
                .membershipTypeId(b.getMembershipType().getId())
                .membershipTypeName(b.getMembershipType().getName())
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .capacity(b.getCapacity())
                .equipmentType(b.getEquipmentType())
                .equipmentQuantity(b.getEquipmentQuantity())
                .customerName(b.getCustomerName())
                .customerPhone(b.getCustomerPhone())
                .notes(b.getNotes())
                .paymentReceived(b.getPaymentReceived())
                .arrived(b.getArrived())
                .createdByName(b.getCreatedBy().getFullName())
                .createdAt(b.getCreatedAt())
                .build();
    }
}