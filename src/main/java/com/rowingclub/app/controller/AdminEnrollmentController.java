package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.EnrollmentResponse;
import com.rowingclub.app.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/enrollments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANTRENÖR')")
public class AdminEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getSessionEnrollments(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(
                ApiResponse.success(enrollmentService.getSessionEnrollments(sessionId))
        );
    }
    @PutMapping("/{enrollmentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelEnrollment(
            @PathVariable UUID enrollmentId) {
        enrollmentService.adminCancelEnrollment(enrollmentId);
        return ResponseEntity.ok(ApiResponse.success("Kayıt iptal edildi", null));
    }

    @PutMapping("/{enrollmentId}/attendance")
    public ResponseEntity<ApiResponse<Void>> toggleAttendance(
            @PathVariable UUID enrollmentId) {
        enrollmentService.toggleAttendance(enrollmentId);
        return ResponseEntity.ok(ApiResponse.success("Yoklama güncellendi", null));
    }

    @PostMapping("/sessions/{sessionId}/users/{userId}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> addParticipant(
            @PathVariable UUID sessionId,
            @PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Katılımcı eklendi",
                        enrollmentService.adminEnroll(sessionId, userId)));
    }

    @GetMapping("/memberships/{membershipId}")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getByMembership(
            @PathVariable UUID membershipId) {
        return ResponseEntity.ok(
                ApiResponse.success(enrollmentService.getByMembership(membershipId)));
    }
}