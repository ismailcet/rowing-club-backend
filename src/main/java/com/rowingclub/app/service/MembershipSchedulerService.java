package com.rowingclub.app.service;

import com.rowingclub.app.entity.Membership;
import com.rowingclub.app.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipSchedulerService {

    private final MembershipRepository membershipRepository;
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
}