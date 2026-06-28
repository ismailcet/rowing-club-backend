package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.ClubInfoResponse;
import com.rowingclub.app.dto.ClubInfoUpdateRequest;
import com.rowingclub.app.service.ClubInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/club-info")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClubInfoController {

    private final ClubInfoService clubInfoService;

    @GetMapping
    public ResponseEntity<ApiResponse<ClubInfoResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(clubInfoService.get()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ClubInfoResponse>> update(
            @Valid @RequestBody ClubInfoUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Kulüp bilgileri güncellendi",
                        clubInfoService.update(request)));
    }
}