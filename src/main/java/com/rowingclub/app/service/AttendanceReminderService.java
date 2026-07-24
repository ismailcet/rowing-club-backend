package com.rowingclub.app.service;

import com.rowingclub.app.entity.Session;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.EnrollmentRepository;
import com.rowingclub.app.repository.SessionRepository;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceReminderService {

    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 12,21 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void scheduledCheck() {
        int count = checkAndNotify();
        log.info("Yoklama hatırlatması: {} dersin yoklaması eksik", count);
    }

    @Transactional
    public int checkAndNotify() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Session> todaysSessions = sessionRepository.findAllByDateRange(today, today);
        int incompleteCount = 0;
        for (Session s : todaysSessions) {
            if (s.getStatus() != Session.SessionStatus.SCHEDULED) {
                continue;
            }
            if (s.getEndTime().isAfter(now)) {
                continue;
            }
            boolean hasUnmarked = enrollmentRepository.findActiveEnrollmentsBySessionId(s.getId())
                    .stream()
                    .anyMatch(e -> e.getIsAttended() == null);
            if (hasUnmarked) {
                incompleteCount++;
            }
        }

        if (incompleteCount > 0) {
            List<User> trainers = userRepository
                    .findAllByUserTypeNameAndCanManageAttendanceTrueAndDeletedFalse("ANTRENÖR");
            final int finalCount = incompleteCount;
            trainers.forEach(t -> notificationService.sendAttendanceReminder(t, finalCount));
        }

        return incompleteCount;
    }
}