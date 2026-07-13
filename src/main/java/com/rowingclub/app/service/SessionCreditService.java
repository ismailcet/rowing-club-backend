package com.rowingclub.app.service;

import com.rowingclub.app.entity.Enrollment;
import com.rowingclub.app.entity.Membership;
import com.rowingclub.app.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionCreditService {

    private final MembershipRepository membershipRepository;

    @Transactional
    public void refundSession(Enrollment enrollment) {
        Membership own = enrollment.getMembership();

        if (own.getStatus() == Membership.MembershipStatus.ACTIVE) {
            own.setSessionsRemaining(own.getSessionsRemaining() + 1);
            membershipRepository.save(own);
            return;
        }

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

        own.setSessionsRemaining(own.getSessionsRemaining() + 1);
        if (own.getStatus() == Membership.MembershipStatus.COMPLETED) {
            own.setStatus(Membership.MembershipStatus.ACTIVE);
        }
        membershipRepository.save(own);
    }

    private boolean coversBranch(Membership m, UUID membershipTypeId) {
        return m.getPlan().getPlanTypes().stream()
                .anyMatch(pt -> pt.getMembershipType().getId().equals(membershipTypeId));
    }
}