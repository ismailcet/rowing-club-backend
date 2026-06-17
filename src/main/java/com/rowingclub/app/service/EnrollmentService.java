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
                .filter(m -> m.getPlan().getPlanTypes().stream()
                        .anyMatch(pt -> pt.getMembershipType().getId().equals(membershipTypeId)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Bu ders için geçerli aktif üyeliğiniz bulunmamaktadır !",
                        HttpStatus.FORBIDDEN
                ));

        if(membership.getSessionsRemaining() == 0){
            throw new BusinessException(
                    "Bu ders için yeterli ders hakkınız yok",
                    HttpStatus.FORBIDDEN
            );
        }

        // Kontejan ve kota düş
        session.setCurrentCapacity(session.getCurrentCapacity() + 1);
        membership.setSessionsRemaining(membership.getSessionsRemaining() - 1);

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

        // Kontejan ve kota iade et
        var session = enrollment.getSession();
        session.setCurrentCapacity(session.getCurrentCapacity() - 1);

        var membership = enrollment.getMembership();
        membership.setSessionsRemaining(membership.getSessionsRemaining() + 1);

        enrollment.setStatus(Enrollment.EnrollmentStatus.CANCELLED);

        sessionRepository.save(session);
        membershipRepository.save(membership);
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
        var membership = enrollment.getMembership();
        membership.setSessionsRemaining(membership.getSessionsRemaining() + 1);
        enrollment.setStatus(Enrollment.EnrollmentStatus.CANCELLED);
        sessionRepository.save(session);
        membershipRepository.save(membership);
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void toggleAttendance(UUID enrollmentId) {
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));
        enrollment.setIsAttended(!enrollment.getIsAttended());
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
                .filter(m -> m.getPlan().getPlanTypes().stream()
                        .anyMatch(pt -> pt.getMembershipType().getId().equals(membershipTypeId)))
                .filter(m -> m.getSessionsRemaining() > 0)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Bu kullanıcının bu ders için geçerli aktif üyeliği yok",
                        HttpStatus.FORBIDDEN
                ));

        session.setCurrentCapacity(session.getCurrentCapacity() + 1);
        membership.setSessionsRemaining(membership.getSessionsRemaining() - 1);

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