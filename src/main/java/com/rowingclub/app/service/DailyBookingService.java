package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.CreateDailyBookingRequest;
import com.rowingclub.app.dto.DailyBookingResponse;
import com.rowingclub.app.dto.UpdateDailyBookingStatusRequest;
import com.rowingclub.app.entity.DailyBooking;
import com.rowingclub.app.entity.MembershipType;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.DailyBookingRepository;
import com.rowingclub.app.repository.MembershipTypeRepository;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyBookingService {

    private final DailyBookingRepository dailyBookingRepository;
    private final MembershipTypeRepository membershipTypeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DailyBookingResponse> getForDate(LocalDate date) {
        return dailyBookingRepository.findAllByBookingDateOrderByStartTime(date)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDates(LocalDate startDate, LocalDate endDate) {
        return dailyBookingRepository.findDistinctDatesInRange(startDate, endDate)
                .stream()
                .map(LocalDate::toString)
                .collect(Collectors.toList());
    }

    @Transactional
    public DailyBookingResponse create(CreateDailyBookingRequest request, UUID creatorId) {
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
        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new BusinessException("Kişi sayısı en az 1 olmalı", HttpStatus.BAD_REQUEST);
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        DailyBooking booking = DailyBooking.builder()
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
                .build();

        dailyBookingRepository.save(booking);
        return toResponse(booking);
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