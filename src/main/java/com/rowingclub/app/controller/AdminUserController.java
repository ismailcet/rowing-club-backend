package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.AuthResponse;
import com.rowingclub.app.dto.CreateUserRequest;
import com.rowingclub.app.dto.ResetPasswordRequest;
import com.rowingclub.app.dto.UserResponse;
import com.rowingclub.app.service.AuthService;
import com.rowingclub.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuthResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Kullanıcı oluşturuldu", authService.createUser(request))
        );
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/type/{userTypeName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANTRENÖR')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByType(
            @PathVariable String userTypeName) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getUsersByType(userTypeName))
        );
    }

    @PutMapping("/{userId}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleActive(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(
                ApiResponse.success("Kullanıcı durumu güncellendi", userService.toggleUserActive(userId))
        );
    }

    @PutMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Şifre sıfırlandı", null));
    }
}