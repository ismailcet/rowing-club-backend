package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.EnrollmentResponse;
import com.rowingclub.app.entity.*;
import com.rowingclub.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final SessionRepository sessionRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final SettingService settingService;
    private final ProgressService progressService;
    private final NotificationService notificationService;
    private final SessionCreditService sessionCreditService;

    private static final int LOW_SESSIONS_THRESHOLD = 2;

    @Transactional
    public EnrollmentResponse enroll(UUID userId, UUID sessionId) {

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        if (session.getStatus() != Session.SessionStatus.SCHEDULED) {
            throw new BusinessException("Bu derse kayıt yapılamaz, ders aktif değil", HttpStatus.BAD_REQUEST);
        }

        if (isPastSession(session)) {
            throw new BusinessException("Geçmiş tarihli derse kayıt yapılamaz", HttpStatus.BAD_REQUEST);
        }


        if (enrollmentRepository.existsByUserIdAndSessionIdAndStatus(
                userId, sessionId, Enrollment.EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Bu derse zaten kayıtlısınız", HttpStatus.CONFLICT);
        }

        UUID membershipTypeId = session.getTemplate().getMembershipType().getId();

        // Bu branşı kapsayan aktif üyelikler
        List<Membership> covering = membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE)
                .stream()
                .filter(m -> coversBranch(m, membershipTypeId))
                .collect(Collectors.toList());

        if (covering.isEmpty()) {
            throw new BusinessException(
                    "Bu ders için geçerli aktif üyeliğiniz bulunmamaktadır !",
                    HttpStatus.FORBIDDEN
            );
        }

        // Eğitim üyeliği önceliklidir; kullanılabilir (hak var + tarih uygun) olanı seç.
        Membership membership = pickMembershipForEnroll(covering, session);
        if (membership == null) {
            throw new BusinessException(
                    "Bu ders için yeterli ders hakkınız yok ya da üyelik tarihiniz uygun değil",
                    HttpStatus.FORBIDDEN
            );
        }

        boolean useTraining = Boolean.TRUE.equals(membership.getPlan().getIsTraining());

        // Kapasite kontrolü ve düşüş (doğru kova)
        if (useTraining) {
            if (session.isTrainingFull()) {
                throw new BusinessException("Bu dersin eğitim kontenjanı doldu", HttpStatus.CONFLICT);
            }
            session.setCurrentTrainingCapacity(session.getCurrentTrainingCapacity() + 1);
        } else {
            if (session.isFull()) {
                throw new BusinessException("Bu dersin kontenjani doldu", HttpStatus.CONFLICT);
            }
            session.setCurrentCapacity(session.getCurrentCapacity() + 1);
        }

        consumeSession(membership);

        sessionRepository.save(session);
        membershipRepository.save(membership);

        Enrollment enrollment = Enrollment.builder()
                .user(userRepository.getReferenceById(userId))
                .session(session)
                .membership(membership)
                .usedTrainingSlot(useTraining)
                .build();

        enrollmentRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse cancel(UUID userId, UUID enrollmentId) {
        var enrollment = enrollmentRepository.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        if (enrollment.getStatus() == Enrollment.EnrollmentStatus.CANCELLED) {
            throw new BusinessException("Bu kayıt zaten iptal edilmiş", HttpStatus.BAD_REQUEST);
        }

        if (enrollment.getSession().getStatus() != Session.SessionStatus.SCHEDULED) {
            throw new BusinessException("Dersin durumu nedeniyle iptal yapılamaz", HttpStatus.BAD_REQUEST);
        }

        int cancellationDeadlineHours = settingService.getIntValue("CANCELLATION_DEADLINE_HOURS");

        LocalDateTime sessionStart = LocalDateTime.of(
                enrollment.getSession().getSessionDate(),
                enrollment.getSession().getStartTime()
        );
        LocalDateTime cancellationDeadline = sessionStart.minusHours(cancellationDeadlineHours);

        if (LocalDateTime.now().isAfter(cancellationDeadline)) {
            throw new BusinessException(
                    "Derse " + cancellationDeadlineHours + " saatten az süre kaldığı için iptal yapılamaz",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Kontejan iade (kaydın düştüğü kovaya)
        var session = enrollment.getSession();
        if (Boolean.TRUE.equals(enrollment.getUsedTrainingSlot())) {
            session.setCurrentTrainingCapacity(
                    Math.max(0, session.getCurrentTrainingCapacity() - 1));
        } else {
            session.setCurrentCapacity(Math.max(0, session.getCurrentCapacity() - 1));
        }

        // Ders hakkı iade (Yorum A: o branştaki aktif üyeliğe; yoksa kendi üyeliğini dirilt)
        sessionCreditService.refundSession(enrollment);

        enrollment.setStatus(Enrollment.EnrollmentStatus.CANCELLED);

        sessionRepository.save(session);
        enrollmentRepository.save(enrollment);

        return toResponse(enrollment);
    }

    public List<EnrollmentResponse> getMyEnrollments(UUID userId) {
        return enrollmentRepository
                .findAllByUserIdAndStatus(userId, Enrollment.EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EnrollmentResponse> getSessionEnrollments(UUID sessionId) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        UUID branchId = session.getTemplate().getMembershipType().getId();
        String branchName = session.getTemplate().getMembershipType().getName();

        return enrollmentRepository.findActiveEnrollmentsBySessionId(sessionId)
                .stream()
                .map(e -> {
                    EnrollmentResponse r = toResponse(e);
                    var bp = progressService.getBranchProgress(
                            e.getUser().getId(), branchId, branchName);
                    r.setLevelNumber(bp.getLevelNumber());
                    r.setLevelName(bp.getLevelName());
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void adminCancelEnrollment(UUID enrollmentId) {
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));
        var session = enrollment.getSession();
        session.setCurrentCapacity(session.getCurrentCapacity() - 1);

        sessionCreditService.refundSession(enrollment);

        enrollment.setStatus(Enrollment.EnrollmentStatus.CANCELLED);
        sessionRepository.save(session);
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void setAttendance(UUID enrollmentId, Boolean attended) {
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));
        enrollment.setIsAttended(attended);
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public EnrollmentResponse adminEnroll(UUID sessionId, UUID userId) {
        return toResponse(enrollmentRepository.save(
                buildEnrollment(userId, sessionId)
        ));
    }

    private Enrollment buildEnrollment(UUID userId, UUID sessionId) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        if (isPastSession(session)) {
            throw new BusinessException("Geçmiş tarihli derse kayıt yapılamaz", HttpStatus.BAD_REQUEST);
        }

        if (enrollmentRepository.existsByUserIdAndSessionIdAndStatus(
                userId, sessionId, Enrollment.EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Bu kullanıcı zaten kayıtlı", HttpStatus.CONFLICT);
        }

        UUID membershipTypeId = session.getTemplate().getMembershipType().getId();

        List<Membership> covering = membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE)
                .stream()
                .filter(m -> coversBranch(m, membershipTypeId))
                .collect(Collectors.toList());

        if (covering.isEmpty()) {
            throw new BusinessException(
                    "Bu kullanıcının bu ders için geçerli aktif üyeliği yok",
                    HttpStatus.FORBIDDEN
            );
        }

        Membership membership = pickMembershipForEnroll(covering, session);
        if (membership == null) {
            throw new BusinessException(
                    "Bu kullanıcının yeterli ders hakkı yok ya da üyelik tarihi uygun değil",
                    HttpStatus.FORBIDDEN
            );
        }

        boolean useTraining = Boolean.TRUE.equals(membership.getPlan().getIsTraining());

        if (useTraining) {
            if (session.isTrainingFull()) {
                throw new BusinessException("Bu dersin eğitim kontenjanı doldu", HttpStatus.CONFLICT);
            }
            session.setCurrentTrainingCapacity(session.getCurrentTrainingCapacity() + 1);
        } else {
            if (session.isFull()) {
                throw new BusinessException("Bu dersin kontenjani doldu", HttpStatus.CONFLICT);
            }
            session.setCurrentCapacity(session.getCurrentCapacity() + 1);
        }

        consumeSession(membership);

        sessionRepository.save(session);
        membershipRepository.save(membership);

        return Enrollment.builder()
                .user(userRepository.getReferenceById(userId))
                .session(session)
                .membership(membership)
                .usedTrainingSlot(useTraining)
                .build();
    }

    public List<EnrollmentResponse> getByMembership(UUID membershipId) {
        return enrollmentRepository.findByMembershipId(membershipId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Üyenin kendi üyeliğinin rezervasyon geçmişi (sahiplik kontrolü ile). */
    public List<EnrollmentResponse> getMyMembershipEnrollments(UUID userId, UUID membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership", "id", membershipId));
        if (!membership.getUser().getId().equals(userId)) {
            throw new BusinessException("Bu üyelik size ait değil", HttpStatus.FORBIDDEN);
        }
        return getByMembership(membershipId);
    }

    // ---------- Üyelik yaşam döngüsü yardımcıları ----------

    /** Seansın başlangıcı geçmişte mi? */
    private boolean isPastSession(Session session) {
        java.time.LocalDateTime start =
                java.time.LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        return start.isBefore(java.time.LocalDateTime.now());
    }

    /** Üyeliğin planı, verilen branş (membershipType) için geçerli mi? */
    private boolean coversBranch(Membership m, UUID membershipTypeId) {
        return m.getPlan().getPlanTypes().stream()
                .anyMatch(pt -> pt.getMembershipType().getId().equals(membershipTypeId));
    }

    /** Üyelik bu seans için kullanılabilir mi (ders hakkı var + tarih uygun)? */
    private boolean isUsable(Membership m, Session session) {
        return m.getSessionsRemaining() != null
                && m.getSessionsRemaining() > 0
                && !session.getSessionDate().isAfter(m.getEndDate());
    }

    /**
     * Kayıt için kullanılacak üyeliği seçer. Eğitim üyeliği önceliklidir
     * (eğitim paketi sahibi eğitim kontenjanından düşsün diye); yoksa normal üyelik.
     */
    private Membership pickMembershipForEnroll(List<Membership> covering, Session session) {
        Membership training = covering.stream()
                .filter(m -> Boolean.TRUE.equals(m.getPlan().getIsTraining()))
                .filter(m -> isUsable(m, session))
                .findFirst()
                .orElse(null);
        if (training != null) {
            return training;
        }
        return covering.stream()
                .filter(m -> !Boolean.TRUE.equals(m.getPlan().getIsTraining()))
                .filter(m -> isUsable(m, session))
                .findFirst()
                .orElse(null);
    }

    /**
     * Bir ders hakkı düş. Üyelik, ders hakkı 0'a inse bile ACTIVE kalır;
     * COMPLETED'a geçiş artık rezervasyon anında DEĞİL, son rezerve edilen
     * dersin tarihi geçtikten sonra zamanlanmış görev tarafından yapılır
     * (bkz. MembershipSchedulerService#completeFinishedMemberships).
     */
    private void consumeSession(Membership membership) {
        int remaining = membership.getSessionsRemaining() - 1;
        membership.setSessionsRemaining(remaining);
        if (remaining >= 0 && remaining <= LOW_SESSIONS_THRESHOLD) {
            notificationService.sendLowSessionsWarning(membership.getUser(), membership);
        }
    }

    /**
     * İptal iadesi (Yorum A):
     * 1) Kayıtlı olunan üyelik hâlâ ACTIVE ise ona +1.
     * 2) Değilse, o branşta başka bir ACTIVE üyelik varsa ona +1.
     * 3) Hiç aktif yoksa, kayıtlı olunan (COMPLETED) üyeliği +1 ile yeniden ACTIVE yap.
     */
    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .session(sessionService.toSessionResponse(enrollment.getSession()))
                .membershipId(enrollment.getMembership().getId())
                .planName(enrollment.getMembership().getPlan().getName())
                .sessionsRemaining(enrollment.getMembership().getSessionsRemaining())
                .enrolledAt(enrollment.getEnrolledAt())
                .status(enrollment.getStatus().name())
                .isAttended(enrollment.getIsAttended())
                .userId(enrollment.getUser().getId())
                .userFullName(enrollment.getUser().getFullName())
                .userEmail(enrollment.getUser().getEmail())
                .build();
    }
}