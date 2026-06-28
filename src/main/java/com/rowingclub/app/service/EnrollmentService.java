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


        if (session.isFull()) {
            throw new BusinessException("Bu dersin kontenjani doldu", HttpStatus.CONFLICT);
        }

        if (enrollmentRepository.existsByUserIdAndSessionIdAndStatus(
                userId, sessionId, Enrollment.EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Bu derse zaten kayıtlısınız", HttpStatus.CONFLICT);
        }

        UUID membershipTypeId = session.getTemplate().getMembershipType().getId();

        Membership membership = membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE)
                .stream()
                .filter(m -> coversBranch(m, membershipTypeId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Bu ders için geçerli aktif üyeliğiniz bulunmamaktadır !",
                        HttpStatus.FORBIDDEN
                ));

        if (membership.getSessionsRemaining() == 0) {
            throw new BusinessException(
                    "Bu ders için yeterli ders hakkınız yok",
                    HttpStatus.FORBIDDEN
            );
        }

        // Kontejan ve kota düş
        session.setCurrentCapacity(session.getCurrentCapacity() + 1);
        consumeSession(membership);

        sessionRepository.save(session);
        membershipRepository.save(membership);

        Enrollment enrollment = Enrollment.builder()
                .user(userRepository.getReferenceById(userId))
                .session(session)
                .membership(membership)
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

        // Kontejan iade
        var session = enrollment.getSession();
        session.setCurrentCapacity(session.getCurrentCapacity() - 1);

        // Ders hakkı iade (Yorum A: o branştaki aktif üyeliğe; yoksa kendi üyeliğini dirilt)
        refundSession(enrollment);

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
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        return enrollmentRepository.findActiveEnrollmentsBySessionId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void adminCancelEnrollment(UUID enrollmentId) {
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));
        var session = enrollment.getSession();
        session.setCurrentCapacity(session.getCurrentCapacity() - 1);

        refundSession(enrollment);

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

        if (session.isFull()) {
            throw new BusinessException("Bu dersin kontenjani doldu", HttpStatus.CONFLICT);
        }

        if (enrollmentRepository.existsByUserIdAndSessionIdAndStatus(
                userId, sessionId, Enrollment.EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Bu kullanıcı zaten kayıtlı", HttpStatus.CONFLICT);
        }

        UUID membershipTypeId = session.getTemplate().getMembershipType().getId();

        var membership = membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE)
                .stream()
                .filter(m -> coversBranch(m, membershipTypeId))
                .filter(m -> m.getSessionsRemaining() > 0)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Bu kullanıcının bu ders için geçerli aktif üyeliği yok",
                        HttpStatus.FORBIDDEN
                ));

        session.setCurrentCapacity(session.getCurrentCapacity() + 1);
        consumeSession(membership);

        sessionRepository.save(session);
        membershipRepository.save(membership);

        return Enrollment.builder()
                .user(userRepository.getReferenceById(userId))
                .session(session)
                .membership(membership)
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

    /** Bir ders hakkı düş; 0'a inerse üyeliği COMPLETED yap. */
    private void consumeSession(Membership membership) {
        membership.setSessionsRemaining(membership.getSessionsRemaining() - 1);
        if (membership.getSessionsRemaining() <= 0) {
            membership.setStatus(Membership.MembershipStatus.COMPLETED);
        }
    }

    /**
     * İptal iadesi (Yorum A):
     * 1) Kayıtlı olunan üyelik hâlâ ACTIVE ise ona +1.
     * 2) Değilse, o branşta başka bir ACTIVE üyelik varsa ona +1.
     * 3) Hiç aktif yoksa, kayıtlı olunan (COMPLETED) üyeliği +1 ile yeniden ACTIVE yap.
     */
    private void refundSession(Enrollment enrollment) {
        Membership own = enrollment.getMembership();

        // 1) Kendi üyeliği hâlâ aktifse ona iade
        if (own.getStatus() == Membership.MembershipStatus.ACTIVE) {
            own.setSessionsRemaining(own.getSessionsRemaining() + 1);
            membershipRepository.save(own);
            return;
        }

        // 2) O branştaki başka bir aktif üyeliğe iade
        UUID membershipTypeId = enrollment.getSession().getTemplate().getMembershipType().getId();
        UUID userId = enrollment.getUser().getId();

        Membership activeInBranch = membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE)
                .stream()
                .filter(m -> coversBranch(m, membershipTypeId))
                .findFirst()
                .orElse(null);

        if (activeInBranch != null) {
            activeInBranch.setSessionsRemaining(activeInBranch.getSessionsRemaining() + 1);
            membershipRepository.save(activeInBranch);
            return;
        }

        // 3) Aktif yoksa kendi üyeliğini dirilt
        own.setSessionsRemaining(own.getSessionsRemaining() + 1);
        if (own.getStatus() == Membership.MembershipStatus.COMPLETED) {
            own.setStatus(Membership.MembershipStatus.ACTIVE);
        }
        membershipRepository.save(own);
    }

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