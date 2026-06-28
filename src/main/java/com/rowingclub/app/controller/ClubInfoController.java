package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.ClubInfoResponse;
import com.rowingclub.app.service.ClubInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/club-info")
@RequiredArgsConstructor
public class ClubInfoController {

    private final ClubInfoService clubInfoService;

    @GetMapping
    public ResponseEntity<ApiResponse<ClubInfoResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(clubInfoService.get()));
    }
}