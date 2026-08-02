package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.AuthResponse;
import com.rowingclub.app.dto.BranchProgressResponse;
import com.rowingclub.app.dto.CreateUserRequest;
import com.rowingclub.app.dto.ResetPasswordRequest;
import com.rowingclub.app.dto.UpdateTrainerBranchesRequest;
import com.rowingclub.app.dto.UpdateTrainerPermissionsRequest;
import com.rowingclub.app.dto.UserResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.AuthService;
import com.rowingclub.app.service.ProgressService;
import com.rowingclub.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;
    private final UserService userService;
    private final ProgressService progressService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuthResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Kullanıcı oluşturuldu", authService.createUser(request))
        );
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANTRENÖR')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @AuthenticationPrincipal User currentUser) {
        // Antrenör "Sporcular" listesini göremiyorsa VE derse üye ekleme yetkisi
        // de yoksa engelle (admin her zaman görür). Üye ekleme akışı (member
        // picker) bu uca gidiyor, o yüzden getUsersByType ile aynı kural.
        if (!"ADMIN".equalsIgnoreCase(currentUser.getUserType().getName())
                && !Boolean.TRUE.equals(currentUser.getCanViewAthletes())
                && !Boolean.TRUE.equals(currentUser.getCanAddParticipants())) {
            throw new AccessDeniedException("Üye listesini görme yetkiniz yok");
        }
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/type/{userTypeName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANTRENÖR')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByType(
            @PathVariable String userTypeName,
            @AuthenticationPrincipal User currentUser) {
        // Antrenör "Sporcular" listesini göremiyorsa VE derse üye ekleme yetkisi
        // de yoksa engelle (admin her zaman görür). İkisinden biri yeterli —
        // ikisi de üye listesine bakmayı gerektiren, ayrı ama meşru sebepler.
        if (!"ADMIN".equalsIgnoreCase(currentUser.getUserType().getName())
                && !Boolean.TRUE.equals(currentUser.getCanViewAthletes())
                && !Boolean.TRUE.equals(currentUser.getCanAddParticipants())) {
            throw new AccessDeniedException("Sporcular listesini görme yetkiniz yok");
        }
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

    /** Admin: kullanıcıyı kalıcı olarak siler (geçmiş kaydı varsa engellenir). */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Kullanıcı silindi", null));
    }

    /** Admin/Antrenör: üyenin branş bazlı gelişim/seviye bilgisi. */
    @GetMapping("/{userId}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANTRENÖR')")
    public ResponseEntity<ApiResponse<List<BranchProgressResponse>>> getUserProgress(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(
                ApiResponse.success(progressService.getProgress(userId))
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

    /** Admin: antrenör yetkilerini (üye listesi / yoklama) günceller. */
    @PutMapping("/{userId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updatePermissions(
            @PathVariable UUID userId,
            @RequestBody UpdateTrainerPermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Yetkiler güncellendi",
                userService.updateTrainerPermissions(userId, request)));
    }

    /** Admin: antrenörün görebileceği branşları günceller. */
    @PutMapping("/{userId}/branches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateBranches(
            @PathVariable UUID userId,
            @RequestBody UpdateTrainerBranchesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Branşlar güncellendi",
                userService.updateTrainerBranches(userId, request)));
    }
}