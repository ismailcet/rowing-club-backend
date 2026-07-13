package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.BroadcastRequest;
import com.rowingclub.app.dto.CreateReminderRuleRequest;
import com.rowingclub.app.dto.ReminderRuleResponse;
import com.rowingclub.app.dto.UpdateReminderRuleRequest;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.service.AttendanceReminderService;
import com.rowingclub.app.service.ReminderRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final ReminderRuleService reminderRuleService;
    private final AttendanceReminderService attendanceReminderService;

    @GetMapping("/reminders")
    public ResponseEntity<ApiResponse<List<ReminderRuleResponse>>> getReminders() {
        return ResponseEntity.ok(ApiResponse.success(reminderRuleService.getAll()));
    }

    @PostMapping("/reminders")
    public ResponseEntity<ApiResponse<ReminderRuleResponse>> createReminder(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateReminderRuleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(reminderRuleService.create(request, admin.getId())));
    }

    @PutMapping("/reminders/{id}")
    public ResponseEntity<ApiResponse<ReminderRuleResponse>> updateReminder(
            @PathVariable UUID id,
            @RequestBody UpdateReminderRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reminderRuleService.update(id, request)));
    }

    @DeleteMapping("/reminders/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReminder(@PathVariable UUID id) {
        reminderRuleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Hatırlatıcı silindi", null));
    }

    @PostMapping("/reminders/{id}/send-now")
    public ResponseEntity<ApiResponse<Void>> sendReminderNow(@PathVariable UUID id) {
        reminderRuleService.sendNow(id);
        return ResponseEntity.ok(ApiResponse.success("Bildirim gönderildi", null));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<Void>> sendBroadcast(@RequestBody BroadcastRequest request) {
        reminderRuleService.sendBroadcast(request);
        return ResponseEntity.ok(ApiResponse.success("Duyuru gönderildi", null));
    }

    @PostMapping("/attendance-reminder/send-now")
    public ResponseEntity<ApiResponse<Integer>> sendAttendanceReminderNow() {
        return ResponseEntity.ok(ApiResponse.success(attendanceReminderService.checkAndNotify()));
    }
}