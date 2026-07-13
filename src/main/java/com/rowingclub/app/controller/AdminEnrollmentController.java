package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.EnrollmentResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/enrollments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANTRENÖR')")
public class AdminEnrollmentController {

    private final EnrollmentService enrollmentService;

    private boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(user.getUserType().getName());
    }

    /** Üye listesi/seans detayını görme yetkisi. */
    private void requireRosterView(User user) {
        if (!isAdmin(user) && !Boolean.TRUE.equals(user.getCanViewRoster())) {
            throw new AccessDeniedException("Üye listesini görme yetkiniz yok");
        }
    }

    /** Yoklama / katılımcı ekleme-çıkarma yetkisi. */
    private void requireAttendance(User user) {
        if (!isAdmin(user) && !Boolean.TRUE.equals(user.getCanManageAttendance())) {
            throw new AccessDeniedException("Yoklama/katılımcı yönetme yetkiniz yok");
        }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getSessionEnrollments(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User user) {
        requireRosterView(user);
        return ResponseEntity.ok(
                ApiResponse.success(enrollmentService.getSessionEnrollments(sessionId))
        );
    }

    @PutMapping("/{enrollmentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelEnrollment(
            @PathVariable UUID enrollmentId,
            @AuthenticationPrincipal User user) {
        requireAttendance(user);
        enrollmentService.adminCancelEnrollment(enrollmentId);
        return ResponseEntity.ok(ApiResponse.success("Kayıt iptal edildi", null));
    }

    // Yoklama: açık değer (true = katıldı, false = katılmadı)
    @PutMapping("/{enrollmentId}/attendance")
    public ResponseEntity<ApiResponse<Void>> setAttendance(
            @PathVariable UUID enrollmentId,
            @RequestParam Boolean attended,
            @AuthenticationPrincipal User user) {
        requireAttendance(user);
        enrollmentService.setAttendance(enrollmentId, attended);
        return ResponseEntity.ok(ApiResponse.success("Yoklama güncellendi", null));
    }

    @PostMapping("/sessions/{sessionId}/users/{userId}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> addParticipant(
            @PathVariable UUID sessionId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User user) {
        requireAttendance(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Katılımcı eklendi",
                        enrollmentService.adminEnroll(sessionId, userId)));
    }

    @GetMapping("/memberships/{membershipId}")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getByMembership(
            @PathVariable UUID membershipId,
            @AuthenticationPrincipal User user) {
        requireRosterView(user);
        return ResponseEntity.ok(
                ApiResponse.success(enrollmentService.getByMembership(membershipId)));
    }
}