package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.*;
import com.rowingclub.app.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANTRENÖR')")
public class AdminSessionController {

    private final SessionService sessionService;

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<SessionTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateSessionTemplateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Şablon oluşturuldu", sessionService.createTemplate(request)));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<SessionTemplateResponse>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getAllTemplates()));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateTemplate(@PathVariable UUID id) {
        sessionService.deactivateTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Şablon deaktif edildi", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getAllSessions(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = startDate.plusDays(7);

        return ResponseEntity.ok(ApiResponse.success(sessionService.getAllSessions(startDate, endDate)));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generateWeeklySessions() {
        int count = sessionService.createWeeklySessionsManually();
        return ResponseEntity.ok(
                ApiResponse.success(count + " session oluşturuldu", null)
        );
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<SessionTemplateResponse>> updateTemplate(
            @PathVariable UUID id,
            @RequestBody UpdateSessionTemplateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Şablon güncellendi", sessionService.updateTemplate(id, request))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> updateSession(
            @PathVariable UUID id,
            @RequestBody UpdateSessionRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Session güncellendi", sessionService.updateSession(id, request))
        );
    }
}