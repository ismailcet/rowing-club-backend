package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.PaymentResponse;
import com.rowingclub.app.dto.RejectPaymentRequest;
import com.rowingclub.app.entity.*;
import com.rowingclub.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Transactional
    public PaymentResponse createCashPayment(UUID userId, UUID planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipPlan", "id", planId));

        checkMembershipConflict(userId, plan);

        Membership membership = membershipRepository.save(
                Membership.builder()
                        .user(user)
                        .plan(plan)
                        .sessionsRemaining(plan.getSessionsIncluded())
                        .status(Membership.MembershipStatus.PENDING_APPROVAL)
                        .build()
        );

        Payment payment = paymentRepository.save(
                Payment.builder()
                        .user(user)
                        .plan(plan)
                        .membership(membership)
                        .amount(plan.getPrice())
                        .paymentMethod(Payment.PaymentMethod.CASH)
                        .build()
        );

        notificationService.sendCashPaymentPendingToAdmins(user, plan, payment);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse createEftPayment(UUID userId, UUID planId, MultipartFile receiptFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipPlan", "id", planId));

        checkMembershipConflict(userId, plan);

        String receiptPath = fileStorageService.storeReceipt(receiptFile);

        Membership membership = membershipRepository.save(
                Membership.builder()
                        .user(user)
                        .plan(plan)
                        .sessionsRemaining(plan.getSessionsIncluded())
                        .status(Membership.MembershipStatus.PENDING_APPROVAL)
                        .build()
        );

        Payment payment = paymentRepository.save(
                Payment.builder()
                        .user(user)
                        .plan(plan)
                        .membership(membership)
                        .amount(plan.getPrice())
                        .paymentMethod(Payment.PaymentMethod.EFT)
                        .receiptPath(receiptPath)
                        .build()
        );

        notificationService.sendEftPaymentPendingToAdmins(user, plan, payment);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse approve(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new BusinessException("Bu ödeme zaten işlenmiş", HttpStatus.BAD_REQUEST);
        }

        Membership membership = payment.getMembership();
        membership.setStartDate(LocalDate.now());
        membership.setEndDate(LocalDate.now().plusDays(payment.getPlan().getDurationDays()));
        membership.setStatus(Membership.MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        notificationService.sendPaymentApproved(payment.getUser(), membership);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse reject(UUID paymentId, RejectPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new BusinessException("Bu ödeme zaten işlenmiş", HttpStatus.BAD_REQUEST);
        }

        Membership membership = payment.getMembership();
        membership.setStatus(Membership.MembershipStatus.CANCELLED);
        membershipRepository.save(membership);

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setRejectionReason(request.getReason());
        paymentRepository.save(payment);

        notificationService.sendPaymentRejected(payment.getUser(), membership, request.getReason());
        return toResponse(payment);
    }

    public List<PaymentResponse> getPendingPayments() {
        return paymentRepository.findAllByStatus(Payment.PaymentStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getMyPayments(UUID userId) {
        return paymentRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void checkMembershipConflict(UUID userId, MembershipPlan plan) {
        for (MembershipPlanType planType : plan.getPlanTypes()) {
            UUID membershipTypeId = planType.getMembershipType().getId();
            if (membershipRepository.existsActiveMembershipForType(userId, membershipTypeId)) {
                throw new BusinessException(
                        planType.getMembershipType().getName()
                                + " tipinde zaten aktif veya onay bekleyen bir üyeliğiniz var",
                        HttpStatus.CONFLICT
                );
            }
        }
    }


    public Payment getPaymentEntityById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUser().getId())
                .userFullName(payment.getUser().getFullName())
                .userEmail(payment.getUser().getEmail())
                .planId(payment.getPlan().getId())
                .planName(payment.getPlan().getName())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .status(payment.getStatus().name())
                .rejectionReason(payment.getRejectionReason())
                .hasReceipt(payment.getReceiptPath() != null)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}