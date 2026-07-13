package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.BroadcastRequest;
import com.rowingclub.app.dto.CreateReminderRuleRequest;
import com.rowingclub.app.dto.ReminderRuleResponse;
import com.rowingclub.app.dto.UpdateReminderRuleRequest;
import com.rowingclub.app.entity.ReminderRule;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.ReminderRuleRepository;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderRuleService {

    private final ReminderRuleRepository reminderRuleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public List<ReminderRuleResponse> getAll() {
        return reminderRuleRepository.findAllWithDetails()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReminderRuleResponse create(CreateReminderRuleRequest request, UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("Başlık zorunludur", HttpStatus.BAD_REQUEST);
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException("Mesaj zorunludur", HttpStatus.BAD_REQUEST);
        }
        if (request.getTimes() == null || request.getTimes().isEmpty()) {
            throw new BusinessException("En az bir saat girilmelidir", HttpStatus.BAD_REQUEST);
        }

        ReminderRule.TargetType targetType = parseTargetType(request.getTargetType());
        validateTarget(targetType, request.getTargetRole(), request.getTargetUserId());

        User targetUser = null;
        if (targetType == ReminderRule.TargetType.USER) {
            targetUser = userRepository.findById(request.getTargetUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "id", request.getTargetUserId()));
        }

        ReminderRule rule = ReminderRule.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .targetType(targetType)
                .targetRole(targetType == ReminderRule.TargetType.ROLE ? request.getTargetRole() : null)
                .targetUser(targetUser)
                .times(String.join(",", request.getTimes()))
                .daysOfWeek(joinDays(request.getDaysOfWeek()))
                .isActive(request.getIsActive() == null || request.getIsActive())
                .createdBy(admin)
                .build();

        reminderRuleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public ReminderRuleResponse update(UUID ruleId, UpdateReminderRuleRequest request) {
        ReminderRule rule = reminderRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("ReminderRule", "id", ruleId));

        if (request.getTitle() != null) {
            rule.setTitle(request.getTitle());
        }
        if (request.getMessage() != null) {
            rule.setMessage(request.getMessage());
        }
        if (request.getTargetType() != null) {
            ReminderRule.TargetType targetType = parseTargetType(request.getTargetType());
            validateTarget(targetType, request.getTargetRole(), request.getTargetUserId());
            rule.setTargetType(targetType);
            rule.setTargetRole(targetType == ReminderRule.TargetType.ROLE ? request.getTargetRole() : null);
            if (targetType == ReminderRule.TargetType.USER) {
                User targetUser = userRepository.findById(request.getTargetUserId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User", "id", request.getTargetUserId()));
                rule.setTargetUser(targetUser);
            } else {
                rule.setTargetUser(null);
            }
        }
        if (request.getTimes() != null && !request.getTimes().isEmpty()) {
            rule.setTimes(String.join(",", request.getTimes()));
        }
        if (request.getDaysOfWeek() != null) {
            rule.setDaysOfWeek(joinDays(request.getDaysOfWeek()));
        }
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }

        reminderRuleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public void delete(UUID ruleId) {
        if (!reminderRuleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("ReminderRule", "id", ruleId);
        }
        reminderRuleRepository.deleteById(ruleId);
    }

    @Transactional
    public void sendNow(UUID ruleId) {
        ReminderRule rule = reminderRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("ReminderRule", "id", ruleId));
        fireRule(rule);
    }

    @Transactional
    public void sendBroadcast(BroadcastRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("Başlık zorunludur", HttpStatus.BAD_REQUEST);
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException("Mesaj zorunludur", HttpStatus.BAD_REQUEST);
        }
        ReminderRule.TargetType targetType = parseTargetType(request.getTargetType());
        validateTarget(targetType, request.getTargetRole(), request.getTargetUserId());

        List<User> targets = resolveTargetUsers(targetType, request.getTargetRole(), request.getTargetUserId());
        targets.forEach(u -> notificationService.sendBroadcast(u, request.getTitle(), request.getMessage()));
    }

    @Scheduled(cron = "0 * * * * *", zone = "Europe/Istanbul")
    @Transactional
    public void checkAndFireDueRules() {
        LocalDateTime now = LocalDateTime.now();
        String nowHm = now.format(TIME_FORMAT);
        int isoDay = now.getDayOfWeek().getValue();

        List<ReminderRule> dueRules = reminderRuleRepository.findAllByIsActiveTrue()
                .stream()
                .filter(r -> Arrays.asList(r.getTimes().split(",")).contains(nowHm))
                .filter(r -> r.getDaysOfWeek() == null || r.getDaysOfWeek().isBlank()
                        || Arrays.asList(r.getDaysOfWeek().split(",")).contains(String.valueOf(isoDay)))
                .filter(r -> r.getLastSentAt() == null
                        || r.getLastSentAt().isBefore(now.minusSeconds(50)))
                .collect(Collectors.toList());

        for (ReminderRule rule : dueRules) {
            fireRule(rule);
        }
    }

    private void fireRule(ReminderRule rule) {
        List<User> targets = resolveTargetUsers(
                rule.getTargetType(),
                rule.getTargetRole(),
                rule.getTargetUser() != null ? rule.getTargetUser().getId() : null);
        targets.forEach(u -> notificationService.sendBroadcast(u, rule.getTitle(), rule.getMessage()));
        rule.setLastSentAt(LocalDateTime.now());
        reminderRuleRepository.save(rule);
    }

    private List<User> resolveTargetUsers(
            ReminderRule.TargetType targetType, String targetRole, UUID targetUserId) {
        return switch (targetType) {
            case ALL -> userRepository.findAll();
            case ROLE -> userRepository.findAllByUserTypeName(targetRole);
            case USER -> {
                if (targetUserId == null) {
                    yield new ArrayList<>();
                }
                yield userRepository.findById(targetUserId)
                        .map(List::of)
                        .orElse(new ArrayList<>());
            }
        };
    }

    private String joinDays(List<Integer> daysOfWeek) {
        return daysOfWeek == null || daysOfWeek.isEmpty()
                ? null
                : daysOfWeek.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private ReminderRule.TargetType parseTargetType(String raw) {
        try {
            return ReminderRule.TargetType.valueOf(raw);
        } catch (Exception e) {
            throw new BusinessException(
                    "Geçersiz hedef türü. Geçerli değerler: ALL, ROLE, USER", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateTarget(ReminderRule.TargetType targetType, String targetRole, UUID targetUserId) {
        if (targetType == ReminderRule.TargetType.ROLE
                && (targetRole == null || targetRole.isBlank())) {
            throw new BusinessException("Rol hedefinde targetRole zorunludur", HttpStatus.BAD_REQUEST);
        }
        if (targetType == ReminderRule.TargetType.USER && targetUserId == null) {
            throw new BusinessException("Kişi hedefinde targetUserId zorunludur", HttpStatus.BAD_REQUEST);
        }
    }

    private ReminderRuleResponse toResponse(ReminderRule rule) {
        return ReminderRuleResponse.builder()
                .id(rule.getId())
                .title(rule.getTitle())
                .message(rule.getMessage())
                .targetType(rule.getTargetType().name())
                .targetRole(rule.getTargetRole())
                .targetUserId(rule.getTargetUser() != null ? rule.getTargetUser().getId() : null)
                .targetUserName(rule.getTargetUser() != null ? rule.getTargetUser().getFullName() : null)
                .times(Arrays.asList(rule.getTimes().split(",")))
                .daysOfWeek(rule.getDaysOfWeek() == null || rule.getDaysOfWeek().isBlank()
                        ? null
                        : Arrays.stream(rule.getDaysOfWeek().split(","))
                          .map(Integer::parseInt)
                          .collect(Collectors.toList()))
                .isActive(rule.getIsActive())
                .createdByName(rule.getCreatedBy().getFullName())
                .createdAt(rule.getCreatedAt())
                .lastSentAt(rule.getLastSentAt())
                .build();
    }
}