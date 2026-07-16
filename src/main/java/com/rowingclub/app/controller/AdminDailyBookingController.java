package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.CreateDailyBookingRequest;
import com.rowingclub.app.dto.DailyBookingResponse;
import com.rowingclub.app.dto.UpdateDailyBookingStatusRequest;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.DailyBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/daily-bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANTRENÖR')")
public class AdminDailyBookingController {

    private final DailyBookingService dailyBookingService;

    private boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(user.getUserType().getName());
    }

    private void requireDailyBookingPermission(User user) {
        if (!isAdmin(user) && !Boolean.TRUE.equals(user.getCanManageDailyBookings())) {
            throw new AccessDeniedException("Günlük rezervasyon oluşturma yetkiniz yok");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyBookingResponse>>> getForDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(dailyBookingService.getForDate(date)));
    }

    @GetMapping("/dates")
    public ResponseEntity<ApiResponse<List<String>>> getDates(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(dailyBookingService.getDates(startDate, endDate)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DailyBookingResponse>> create(
            @AuthenticationPrincipal User user,
            @RequestBody CreateDailyBookingRequest request) {
        requireDailyBookingPermission(user);
        return ResponseEntity.ok(
                ApiResponse.success("Rezervasyon oluşturuldu",
                        dailyBookingService.create(request, user.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireDailyBookingPermission(user);
        dailyBookingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Rezervasyon silindi", null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DailyBookingResponse>> updateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody UpdateDailyBookingStatusRequest request) {
        requireDailyBookingPermission(user);
        return ResponseEntity.ok(
                ApiResponse.success(dailyBookingService.updateStatus(id, request)));
    }
}