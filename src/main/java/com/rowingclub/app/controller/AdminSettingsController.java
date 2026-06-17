package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.common.SettingKeys;
import com.rowingclub.app.dto.SettingsResponse;
import com.rowingclub.app.dto.SettingsUpdateRequest;
import com.rowingclub.app.service.SettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin · ayar yönetimi. /api/admin/** zaten admin-only olduğundan
 * ekstra güvenlik kuralı gerekmez.
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SettingService settingService;

    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> get() {
        int hours = settingService.getIntValue(SettingKeys.CANCELLATION_DEADLINE_HOURS);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Ayarlar", new SettingsResponse(hours)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> update(
            @Valid @RequestBody SettingsUpdateRequest request) {
        settingService.updateValue(
                SettingKeys.CANCELLATION_DEADLINE_HOURS,
                String.valueOf(request.cancellationDeadlineHours()));
        int hours = settingService.getIntValue(SettingKeys.CANCELLATION_DEADLINE_HOURS);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Ayarlar güncellendi", new SettingsResponse(hours)));
    }
}