package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.UpdateProfileRequest;
import com.rowingclub.app.dto.UserResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getProfile(user.getId()))
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Profil güncellendi", userService.updateProfile(user.getId(), request))
        );
    }
}