package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.BranchProgressResponse;
import com.rowingclub.app.dto.EnrollmentResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.EnrollmentService;
import com.rowingclub.app.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final ProgressService progressService;


    @GetMapping("/my/progress")
    public ResponseEntity<ApiResponse<List<BranchProgressResponse>>> getMyProgress(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success(progressService.getProgress(user.getId()))
        );
    }

    @PostMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Derse kayıt başarılı", enrollmentService.enroll(user.getId(), sessionId)));
    }

    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable UUID enrollmentId) {
        return ResponseEntity.ok(
                ApiResponse.success("Kayıt iptal edildi", enrollmentService.cancel(user.getId(), enrollmentId))
        );
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getMyEnrollments(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success(enrollmentService.getMyEnrollments(user.getId()))
        );
    }

    @GetMapping("/memberships/{membershipId}")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getMyMembershipEnrollments(
            @AuthenticationPrincipal User user,
            @PathVariable UUID membershipId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        enrollmentService.getMyMembershipEnrollments(user.getId(), membershipId))
        );
    }
}