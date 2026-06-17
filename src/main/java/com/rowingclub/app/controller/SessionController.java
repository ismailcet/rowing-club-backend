package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.SessionResponse;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getMySessions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = startDate.plusDays(7);

        return ResponseEntity.ok(
                ApiResponse.success(sessionService.getSessionsForUser(user.getId(), startDate, endDate))
        );
    }
}