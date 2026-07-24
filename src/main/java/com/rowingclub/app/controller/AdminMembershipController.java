package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.*;
import com.rowingclub.app.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/memberships")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMembershipController {

    private final MembershipService membershipService;

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<MembershipPlanResponse>> createPlan(
            @Valid @RequestBody CreateMembershipPlanRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan oluşturuldu", membershipService.createPlan(request)));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<MembershipPlanResponse>>> getAllPlans() {
        return ResponseEntity.ok(
                ApiResponse.success(membershipService.getAllPlans())
        );
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<Void>> deactivatePlan(@PathVariable UUID planId) {
        membershipService.deactivatePlan(planId);
        return ResponseEntity.ok(ApiResponse.success("Plan deaktif edildi", null));
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<MembershipResponse>> assignMembership(
            @Valid @RequestBody AssignMembershipRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Üyelik atandı", membershipService.assignMembership(request)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getUserMemberships(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(
                ApiResponse.success(membershipService.getUserMemberships(userId))
        );
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<MembershipPlanResponse>> updatePlan(
            @PathVariable UUID planId,
            @RequestBody UpdateMembershipPlanRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Plan güncellendi", membershipService.updatePlan(planId, request))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getAllMemberships()));
    }

    /** Bir üyenin üzerindeki tekil üyeliği (paketi) aktif ↔ pasif (iptal) arasında değiştirir. */
    @PutMapping("/{membershipId}/toggle-active")
    public ResponseEntity<ApiResponse<MembershipResponse>> toggleMembershipActive(
            @PathVariable UUID membershipId) {
        return ResponseEntity.ok(
                ApiResponse.success("Üyelik durumu güncellendi",
                        membershipService.toggleMembershipActive(membershipId))
        );
    }

}