package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.DuplicateResourceException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.*;
import com.rowingclub.app.entity.*;
import com.rowingclub.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionTemplateRepository sessionTemplateRepository;
    private final SessionRepository sessionRepository;
    private final MembershipTypeRepository membershipTypeRepository;
    private final MembershipRepository membershipRepository;


    @Transactional
    public SessionTemplateResponse createTemplate(CreateSessionTemplateRequest request) {
        MembershipType membershipType = membershipTypeRepository.findById(request.getMembershipTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", "id", request.getMembershipTypeId()));

        if (sessionTemplateRepository.existsByMembershipTypeIdAndDayOfWeekAndStartTimeAndIsActiveTrue(
                request.getMembershipTypeId(),
                request.getDayOfWeek(),
                request.getStartTime())) {
            throw new DuplicateResourceException(
                    "Bu gün ve saatte aynı tip için zaten aktif bir şablon mevcut"
            );
        }
        SessionTemplate template = SessionTemplate.builder()
                .membershipType(membershipType)
                .name(request.getName())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .capacity(request.getCapacity())
                .build();

        sessionTemplateRepository.save(template);
        return toTemplateResponse(template);
    }

    public List<SessionTemplateResponse> getAllTemplates() {
        return sessionTemplateRepository.findAll()
                .stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateTemplate(UUID templateId) {
        SessionTemplate template = sessionTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionTemplate", "id", templateId));
        template.setIsActive(false);
        sessionTemplateRepository.save(template);
    }

    @Scheduled(cron = "0 0 14 * * SUN", zone = "Europe/Istanbul")
    @Transactional
    public void createWeeklySessions() {
        log.info("Haftalık session oluşturma başladı...");

        LocalDate nextMonday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);
        LocalDate nextSunday = nextMonday.plusDays(6);

        List<SessionTemplate> activeTemplates = sessionTemplateRepository.findAllByIsActiveTrue();
        int created = 0;

        for (SessionTemplate template : activeTemplates) {
            LocalDate sessionDate = nextMonday.plusDays(template.getDayOfWeek() - 1);

            if (sessionRepository.existsByTemplateIdAndSessionDate(template.getId(), sessionDate)) {
                log.info("Session zaten mevcut: {} - {}", template.getName(), sessionDate);
                continue;
            }

            Session session = Session.builder()
                    .template(template)
                    .sessionDate(sessionDate)
                    .startTime(template.getStartTime())
                    .endTime(template.getEndTime())
                    .maxCapacity(template.getCapacity())
                    .currentCapacity(0)
                    .build();

            sessionRepository.save(session);
            created++;
        }

        log.info("Haftalık session oluşturma tamamlandı. Oluşturulan: {}, Dönem: {} - {}",
                created, nextMonday, nextSunday);
    }

    @Transactional
    public int createWeeklySessionsManually() {
        log.info("Manuel haftalık session oluşturma tetiklendi...");

        LocalDate nextMonday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);

        List<SessionTemplate> activeTemplates = sessionTemplateRepository.findAllByIsActiveTrue();
        int created = 0;

        for (SessionTemplate template : activeTemplates) {
            LocalDate sessionDate = nextMonday.plusDays(template.getDayOfWeek() - 1);

            if (sessionRepository.existsByTemplateIdAndSessionDate(template.getId(), sessionDate)) {
                continue;
            }

            Session session = Session.builder()
                    .template(template)
                    .sessionDate(sessionDate)
                    .startTime(template.getStartTime())
                    .endTime(template.getEndTime())
                    .maxCapacity(template.getCapacity())
                    .currentCapacity(0)
                    .build();

            sessionRepository.save(session);
            created++;
        }

        return created;
    }

    public List<SessionResponse> getSessionsForUser(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<UUID> membershipTypeIds = membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE)
                .stream()
                .flatMap(m -> m.getPlan().getPlanTypes().stream())
                .map(pt -> pt.getMembershipType().getId())
                .distinct()
                .collect(Collectors.toList());

        if (membershipTypeIds.isEmpty()) {
            return List.of();
        }

        return sessionRepository
                .findByDateRangeAndMembershipTypes(startDate, endDate, membershipTypeIds)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    public List<SessionResponse> getAllSessions(LocalDate startDate, LocalDate endDate) {
        return sessionRepository.findAllByDateRange(startDate, endDate)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SessionTemplateResponse updateTemplate(UUID templateId, UpdateSessionTemplateRequest request) {
        SessionTemplate template = sessionTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionTemplate", "id", templateId));

        if (request.getMembershipTypeId() != null) {
            MembershipType membershipType = membershipTypeRepository.findById(request.getMembershipTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("MembershipType", "id", request.getMembershipTypeId()));
            template.setMembershipType(membershipType);
        }

        if (sessionTemplateRepository.existsByMembershipTypeIdAndDayOfWeekAndStartTimeAndIsActiveTrue(
                request.getMembershipTypeId(),
                request.getDayOfWeek(),
                request.getStartTime())) {
            throw new DuplicateResourceException(
                    "Bu gün ve saatte aynı tip için zaten aktif bir şablon mevcut"
            );
        }
        if (request.getName() != null) template.setName(request.getName());
        if (request.getDayOfWeek() != null) template.setDayOfWeek(request.getDayOfWeek());
        if (request.getStartTime() != null) template.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) template.setEndTime(request.getEndTime());
        if (request.getCapacity() != null) template.setCapacity(request.getCapacity());
        if (request.getIsActive() != null) template.setIsActive(request.getIsActive());

        sessionTemplateRepository.save(template);
        return toTemplateResponse(template);
    }

    @Transactional
    public SessionResponse updateSession(UUID sessionId, UpdateSessionRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        if (request.getMaxCapacity() != null) {
            if (request.getMaxCapacity() < session.getCurrentCapacity()) {
                throw new BusinessException(
                        "Yeni kontejan mevcut kayıt sayısından (" + session.getCurrentCapacity() + ") az olamaz",
                        HttpStatus.BAD_REQUEST
                );
            }
            session.setMaxCapacity(request.getMaxCapacity());
        }

        if (request.getStatus() != null) {
            try {
                session.setStatus(Session.SessionStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        "Geçersiz session durumu. Geçerli değerler: SCHEDULED, CANCELLED, COMPLETED",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
        sessionRepository.save(session);
        return toSessionResponse(session);
    }


    private SessionTemplateResponse toTemplateResponse(SessionTemplate template) {
        return SessionTemplateResponse.builder()
                .id(template.getId())
                .membershipTypeId(template.getMembershipType().getId())
                .name(template.getName())
                .membershipTypeName(template.getMembershipType().getName())
                .dayOfWeek(template.getDayOfWeek())
                .dayOfWeekLabel(getDayLabel(template.getDayOfWeek()))
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .capacity(template.getCapacity())
                .isActive(template.getIsActive())
                .build();
    }

    public SessionResponse toSessionResponse(Session session) {
        return SessionResponse.builder()
                .id(session.getId())
                .templateName(session.getTemplate().getName())
                .membershipTypeName(session.getTemplate().getMembershipType().getName())
                .sessionDate(session.getSessionDate())
                .dayOfWeekLabel(getDayLabel(session.getSessionDate().getDayOfWeek().getValue()))
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .currentCapacity(session.getCurrentCapacity())
                .maxCapacity(session.getMaxCapacity())
                .remainingCapacity(session.getMaxCapacity() - session.getCurrentCapacity())
                .isFull(session.isFull())
                .status(session.getStatus().name())
                .build();
    }

    private String getDayLabel(int dayOfWeek) {
        return DayOfWeek.of(dayOfWeek)
                .getDisplayName(TextStyle.FULL, new Locale("tr", "TR"));
    }
}