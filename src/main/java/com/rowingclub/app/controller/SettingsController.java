package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.common.SettingKeys;
import com.rowingclub.app.dto.SettingsResponse;
import com.rowingclub.app.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingService settingService;

    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings() {
        int hours = settingService.getIntValue(SettingKeys.CANCELLATION_DEADLINE_HOURS);
        SettingsResponse data = new SettingsResponse(hours);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ayarlar getirildi", data));
    }
}