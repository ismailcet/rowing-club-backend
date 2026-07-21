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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final SessionCreditService sessionCreditService;
    private final TrainerBranchRepository trainerBranchRepository;


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
                .trainingCapacity(request.getTrainingCapacity() != null
                        ? request.getTrainingCapacity() : 0)
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
                    .trainingCapacity(template.getTrainingCapacity())
                    .currentTrainingCapacity(0)
                    .build();

            sessionRepository.save(session);
            created++;
        }

        log.info("Haftalık session oluşturma tamamlandı. Oluşturulan: {}, Dönem: {} - {}",
                created, nextMonday, nextSunday);

        if (created > 0) {
            membershipRepository.findActiveMembershipUsers()
                    .forEach(notificationService::sendWeeklySessionsOpened);
        }
    }

    @Transactional
    public int createWeeklySessionsManually() {
        log.info("Manuel haftalık session oluşturma tetiklendi...");

        LocalDate nextMonday = LocalDate.now().with(DayOfWeek.MONDAY); //.plusWeeks(0);

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
                    .trainingCapacity(template.getTrainingCapacity())
                    .currentTrainingCapacity(0)
                    .build();

            sessionRepository.save(session);
            created++;
        }

        if (created > 0) {
            membershipRepository.findActiveMembershipUsers()
                    .forEach(notificationService::sendWeeklySessionsOpened);
        }

        return created;
    }

    public List<SessionResponse> getSessionsForUser(UUID userId, LocalDate startDate, LocalDate endDate) {
        // Kullanıcının aktif rezervasyonları (branş tamamlansa/iptal olsa bile görünmeli)
        List<Enrollment> myEnrollments = enrollmentRepository
                .findAllByUserIdAndStatus(userId, Enrollment.EnrollmentStatus.ACTIVE);
        Set<UUID> enrolledSessionIds = myEnrollments.stream()
                .map(e -> e.getSession().getId())
                .collect(Collectors.toSet());

        // Kullanıcının aktif üyelikleri (rezerve edilebilirlik için de kullanılır)
        List<Membership> activeMemberships = membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE);

        // Aktif üyelik branşlarındaki seanslar (rezerve edilebilir)
        List<UUID> membershipTypeIds = activeMemberships.stream()
                .flatMap(m -> m.getPlan().getPlanTypes().stream())
                .map(pt -> pt.getMembershipType().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, Session> byId = new LinkedHashMap<>();
        if (!membershipTypeIds.isEmpty()) {
            for (Session s : sessionRepository
                    .findByDateRangeAndMembershipTypes(startDate, endDate, membershipTypeIds)) {
                byId.put(s.getId(), s);
            }
        }

        // Tarih aralığındaki rezerve edilmiş seansları da ekle (listede yoksa)
        for (Enrollment e : myEnrollments) {
            Session s = e.getSession();
            LocalDate d = s.getSessionDate();
            if (!d.isBefore(startDate) && !d.isAfter(endDate)) {
                byId.putIfAbsent(s.getId(), s);
            }
        }

        return byId.values().stream()
                .sorted(Comparator
                        .comparing(Session::getSessionDate)
                        .thenComparing(Session::getStartTime))
                .map(s -> {
                    boolean enrolled = enrolledSessionIds.contains(s.getId());
                    SessionResponse r = toSessionResponse(s, enrolled);
                    r.setReservable(computeReservable(s, activeMemberships, enrolled));
                    return r;
                })
                .collect(Collectors.toList());
    }

    /**
     * Üye takvim noktaları için hafif liste: sadece dolu tarihler
     * (izinli branşlar + kendi kayıtları birleşik). Tam seans verisi
     * çekmeden nokta göstermek için kullanılır.
     */
    public List<String> getSessionDatesForUser(UUID userId, LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> dates = new TreeSet<>();

        // Kendi rezervasyonları (branş tamamlansa/iptal olsa bile görünmeli)
        for (Enrollment e : enrollmentRepository
                .findAllByUserIdAndStatus(userId, Enrollment.EnrollmentStatus.ACTIVE)) {
            LocalDate d = e.getSession().getSessionDate();
            if (!d.isBefore(startDate) && !d.isAfter(endDate)) {
                dates.add(d);
            }
        }

        // Aktif üyelik branşlarındaki tarihler
        List<UUID> membershipTypeIds = membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE)
                .stream()
                .flatMap(m -> m.getPlan().getPlanTypes().stream())
                .map(pt -> pt.getMembershipType().getId())
                .distinct()
                .collect(Collectors.toList());
        if (!membershipTypeIds.isEmpty()) {
            dates.addAll(sessionRepository.findDistinctDatesInRangeAndMembershipTypes(
                    startDate, endDate, membershipTypeIds));
        }

        return dates.stream().map(LocalDate::toString).collect(Collectors.toList());
    }

    /**
     * Bu üye bu seansı rezerve edebilir mi? Eğitim üyeliği önceliklidir:
     * eğitim üyeliği varsa yalnızca eğitim kontenjanı olan ve dolmamış seanslar,
     * yoksa normal üyelikle normal kontenjan dolmadıysa rezerve edilebilir.
     */
    private boolean computeReservable(Session s, List<Membership> memberships, boolean enrolled) {
        if (enrolled) return false;
        if (s.getStatus() != Session.SessionStatus.SCHEDULED) return false;

        UUID branchId = s.getTemplate().getMembershipType().getId();

        boolean usableTraining = memberships.stream().anyMatch(m ->
                Boolean.TRUE.equals(m.getPlan().getIsTraining())
                        && coversBranchType(m, branchId)
                        && isUsableForSession(m, s));
        if (usableTraining) {
            return s.getTrainingCapacity() != null
                    && s.getTrainingCapacity() > 0
                    && !s.isTrainingFull();
        }

        boolean usableNormal = memberships.stream().anyMatch(m ->
                !Boolean.TRUE.equals(m.getPlan().getIsTraining())
                        && coversBranchType(m, branchId)
                        && isUsableForSession(m, s));
        return usableNormal && !s.isFull();
    }

    private boolean coversBranchType(Membership m, UUID branchId) {
        return m.getPlan().getPlanTypes().stream()
                .anyMatch(pt -> pt.getMembershipType().getId().equals(branchId));
    }

    private boolean isUsableForSession(Membership m, Session s) {
        return m.getSessionsRemaining() != null
                && m.getSessionsRemaining() > 0
                && !s.getSessionDate().isAfter(m.getEndDate());
    }

    public List<SessionResponse> getAllSessions(LocalDate startDate, LocalDate endDate, User requester) {
        List<SessionResponse> all = sessionRepository.findAllByDateRange(startDate, endDate)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());

        List<UUID> allowed = allowedBranchIdsOrNull(requester);
        if (allowed == null) {
            return all;
        }
        return all.stream()
                .filter(r -> allowed.contains(r.getMembershipTypeId()))
                .collect(Collectors.toList());
    }

    /** ADMIN veya atama yapılmamış antrenör için null (kısıtlama yok) döner. */
    private List<UUID> allowedBranchIdsOrNull(User requester) {
        if (requester == null || "ADMIN".equalsIgnoreCase(requester.getUserType().getName())) {
            return null;
        }
        List<UUID> assigned = trainerBranchRepository.findMembershipTypeIdsByTrainerId(requester.getId());
        return assigned.isEmpty() ? null : assigned;
    }

    /** Takvim noktaları için hafif liste: sadece dolu tarihler (yyyy-MM-dd). */
    public List<String> getSessionDates(LocalDate startDate, LocalDate endDate) {
        return sessionRepository.findDistinctDatesInRange(startDate, endDate)
                .stream()
                .map(LocalDate::toString)
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

        // Çakışma kontrolü: değişecek (varsa request, yoksa mevcut) değerlerle,
        // şablonun kendisi hariç başka aktif bir şablon var mı?
        UUID effTypeId = request.getMembershipTypeId() != null
                ? request.getMembershipTypeId()
                : template.getMembershipType().getId();
        Integer effDay = request.getDayOfWeek() != null
                ? request.getDayOfWeek()
                : template.getDayOfWeek();
        LocalTime effStart = request.getStartTime() != null
                ? request.getStartTime()
                : template.getStartTime();

        if (sessionTemplateRepository
                .existsByMembershipTypeIdAndDayOfWeekAndStartTimeAndIsActiveTrueAndIdNot(
                        effTypeId, effDay, effStart, templateId)) {
            throw new DuplicateResourceException(
                    "Bu gün ve saatte aynı tip için zaten aktif bir şablon mevcut"
            );
        }
        if (request.getName() != null) template.setName(request.getName());
        if (request.getDayOfWeek() != null) template.setDayOfWeek(request.getDayOfWeek());
        if (request.getStartTime() != null) template.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) template.setEndTime(request.getEndTime());
        if (request.getCapacity() != null) template.setCapacity(request.getCapacity());
        if (request.getTrainingCapacity() != null)
            template.setTrainingCapacity(request.getTrainingCapacity());
        if (request.getIsActive() != null) template.setIsActive(request.getIsActive());

        sessionTemplateRepository.save(template);

        // Değişiklikleri gelecekteki (bugünden sonraki) planlı seanslara yansıt.
        // Geçmiş seanslar ve mevcut kayıtlar korunur (kapasite kayıt sayısının altına inmez).
        List<Session> upcoming = sessionRepository.findUpcomingByTemplate(
                template.getId(), LocalDate.now());
        for (Session s : upcoming) {
            s.setStartTime(template.getStartTime());
            s.setEndTime(template.getEndTime());
            s.setMaxCapacity(
                    Math.max(template.getCapacity(), s.getCurrentCapacity()));
            s.setTrainingCapacity(Math.max(
                    template.getTrainingCapacity(), s.getCurrentTrainingCapacity()));
        }
        if (!upcoming.isEmpty()) {
            sessionRepository.saveAll(upcoming);
        }

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
            Session.SessionStatus oldStatus = session.getStatus();
            Session.SessionStatus newStatus;
            try {
                newStatus = Session.SessionStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        "Geçersiz session durumu. Geçerli değerler: SCHEDULED, CANCELLED, COMPLETED",
                        HttpStatus.BAD_REQUEST
                );
            }
            if (newStatus == Session.SessionStatus.CANCELLED
                    && oldStatus != Session.SessionStatus.CANCELLED
                    && isPastSession(session)) {
                throw new BusinessException(
                        "Geçmiş bir ders iptal edilemez", HttpStatus.BAD_REQUEST);
            }
            session.setStatus(newStatus);
            if (newStatus == Session.SessionStatus.CANCELLED
                    && oldStatus != Session.SessionStatus.CANCELLED) {
                for (Enrollment enrollment
                        : enrollmentRepository.findActiveEnrollmentsBySessionId(sessionId)) {
                    if (Boolean.TRUE.equals(enrollment.getUsedTrainingSlot())) {
                        session.setCurrentTrainingCapacity(
                                Math.max(0, session.getCurrentTrainingCapacity() - 1));
                    } else {
                        session.setCurrentCapacity(Math.max(0, session.getCurrentCapacity() - 1));
                    }
                    sessionCreditService.refundSession(enrollment);
                    enrollment.setStatus(Enrollment.EnrollmentStatus.CANCELLED);
                    enrollmentRepository.save(enrollment);
                    notificationService.sendSessionCancelled(enrollment.getUser(), session);
                }
            }
        }
        sessionRepository.save(session);
        return toSessionResponse(session);
    }

    private boolean isPastSession(Session session) {
        LocalDateTime start = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        return start.isBefore(LocalDateTime.now());
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
                .trainingCapacity(template.getTrainingCapacity())
                .isActive(template.getIsActive())
                .build();
    }

    public SessionResponse toSessionResponse(Session session) {
        return toSessionResponse(session, false);
    }

    public SessionResponse toSessionResponse(Session session, boolean isEnrolled) {
        return SessionResponse.builder()
                .id(session.getId())
                .templateName(session.getTemplate().getName())
                .membershipTypeId(session.getTemplate().getMembershipType().getId())
                .membershipTypeName(session.getTemplate().getMembershipType().getName())
                .sessionDate(session.getSessionDate())
                .dayOfWeekLabel(getDayLabel(session.getSessionDate().getDayOfWeek().getValue()))
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .currentCapacity(session.getCurrentCapacity())
                .maxCapacity(session.getMaxCapacity())
                .remainingCapacity(session.getMaxCapacity() - session.getCurrentCapacity())
                .isFull(session.isFull())
                .currentTrainingCapacity(session.getCurrentTrainingCapacity())
                .trainingCapacity(session.getTrainingCapacity())
                .status(session.getStatus().name())
                .isEnrolled(isEnrolled)
                .build();
    }

    private String getDayLabel(int dayOfWeek) {
        return DayOfWeek.of(dayOfWeek)
                .getDisplayName(TextStyle.FULL, new Locale("tr", "TR"));
    }
}