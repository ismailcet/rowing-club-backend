package com.rowingclub.app.service;

import com.rowingclub.app.entity.Enrollment;
import com.rowingclub.app.entity.Membership;
import com.rowingclub.app.entity.Session;
import com.rowingclub.app.repository.EnrollmentRepository;
import com.rowingclub.app.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipSchedulerService {

    private final MembershipRepository membershipRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void checkMembershipExpirations() {
        LocalDate today = LocalDate.now();
        log.info("Üyelik süresi kontrolü başladı: {}", today);

        notify7Days(today);
        notify3Days(today);
        notify1Day(today);
        expireOverdueMemberships(today);
    }

    private void notify7Days(LocalDate today) {
        LocalDate targetDate = today.plusDays(7);
        List<Membership> memberships = membershipRepository.findMembershipsToNotify7Days(targetDate);

        for (Membership membership : memberships) {
            notificationService.sendMembershipExpiryWarning(membership.getUser(), membership, 7);
            membership.setNotified7Days(true);
            membershipRepository.save(membership);
        }

        log.info("7 gün uyarısı gönderildi: {} üyelik", memberships.size());
    }

    private void notify3Days(LocalDate today) {
        LocalDate targetDate = today.plusDays(3);
        List<Membership> memberships = membershipRepository.findMembershipsToNotify3Days(targetDate);

        for (Membership membership : memberships) {
            notificationService.sendMembershipExpiryWarning(membership.getUser(), membership, 3);
            membership.setNotified3Days(true);
            membershipRepository.save(membership);
        }

        log.info("3 gün uyarısı gönderildi: {} üyelik", memberships.size());
    }

    private void notify1Day(LocalDate today) {
        LocalDate targetDate = today.plusDays(1);
        List<Membership> memberships = membershipRepository.findMembershipsToNotify1Day(targetDate);

        for (Membership membership : memberships) {
            notificationService.sendMembershipExpiryWarning(membership.getUser(), membership, 1);
            membership.setNotified1Day(true);
            membershipRepository.save(membership);
        }

        log.info("1 gün uyarısı gönderildi: {} üyelik", memberships.size());
    }

    private void expireOverdueMemberships(LocalDate today) {
        List<Membership> expiredMemberships = membershipRepository.findExpiredMemberships(today);

        for (Membership membership : expiredMemberships) {
            membership.setStatus(Membership.MembershipStatus.EXPIRED);
            membershipRepository.save(membership);
            notificationService.sendMembershipExpired(membership.getUser(), membership);
        }

        log.info("Süresi dolmuş üyelikler güncellendi: {} üyelik", expiredMemberships.size());
    }

    /**
     * Saat başı üyelik yaşam döngüsü:
     *  1) Süresi (end_date) dolan üyelikler — başka hiçbir koşula bakmaksızın — EXPIRED.
     *  2) Süresi dolmamış; ders hakkı 0 ve son rezerve edilen dersi geçmiş üyelikler COMPLETED.
     * EXPIRED her zaman önceliklidir.
     */
    @Scheduled(cron = "0 5 * * * *", zone = "Europe/Istanbul")
    @Transactional
    public void runHourlyMembershipLifecycle() {
        LocalDate today = LocalDate.now();
        expireOverdueMemberships(today);     // 1) önce süresi dolanlar
        completeFinishedMemberships(today);  // 2) sonra son dersi geçenler
    }

    /**
     * Ders hakkı bitmiş (sessionsRemaining = 0) ama hâlâ ACTIVE olan üyelikleri,
     * son rezerve edilen dersin tarihi/saati geçtikten sonra COMPLETED yapar.
     * Üyeliğin geleceğe dönük (henüz bitmemiş) aktif kaydı varsa ACTIVE kalır.
     * Süresi dolmuş (end_date geçmiş) üyelikler burada atlanır; onları EXPIRED yolu işler.
     */
    private void completeFinishedMemberships(LocalDate today) {
        LocalDateTime now = LocalDateTime.now();
        List<Membership> candidates = membershipRepository.findActiveExhaustedMemberships();

        int completed = 0;
        for (Membership membership : candidates) {
            // Süresi dolmuşsa COMPLETED değil EXPIRED olmalı → atla.
            if (membership.getEndDate().isBefore(today)) {
                continue;
            }

            boolean hasUpcomingSession = enrollmentRepository
                    .findByMembershipId(membership.getId())
                    .stream()
                    .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ACTIVE)
                    .anyMatch(e -> {
                        Session s = e.getSession();
                        LocalDateTime end = LocalDateTime.of(s.getSessionDate(), s.getEndTime());
                        return end.isAfter(now); // hâlâ yapılmamış bir dersi var
                    });

            if (!hasUpcomingSession) {
                membership.setStatus(Membership.MembershipStatus.COMPLETED);
                membershipRepository.save(membership);
                completed++;
            }
        }

        log.info("Tamamlanan üyelikler: {} üyelik", completed);
    }
}