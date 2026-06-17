package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.MembershipPlanResponse;
import com.rowingclub.app.dto.MembershipResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<MembershipPlanResponse>>> getActivePlans() {
        return ResponseEntity.ok(
                ApiResponse.success(membershipService.getAllActivePlans())
        );
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMyMemberships(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success(membershipService.getUserMemberships(user.getId()))
        );
    }
}