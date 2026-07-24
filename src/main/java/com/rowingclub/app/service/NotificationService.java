package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.NotificationResponse;
import com.rowingclub.app.entity.Membership;
import com.rowingclub.app.entity.MembershipPlan;
import com.rowingclub.app.entity.Notification;
import com.rowingclub.app.entity.Payment;
import com.rowingclub.app.entity.Session;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.NotificationRepository;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FcmPushService fcmPushService;

    public void sendMembershipExpiryWarning(User user, Membership membership, int daysLeft) {
        String title = "Üyelik Süresi Yaklaşıyor";
        String body = membership.getPlan().getName()
                + " paketinizin süresi " + daysLeft + " gün sonra dolacaktır.";
        send(user, title, body);
    }

    public void sendMembershipExpired(User user, Membership membership) {
        String title = "Üyeliğiniz Sona Erdi";
        String body = membership.getPlan().getName()
                + " paketinizin süresi dolmuştur. Kulüp hizmetlerinden yararlanmaya devam etmek için yeni bir paket satın alabilirsiniz.";
        send(user, title, body);
    }

    public void sendPaymentApproved(User user, Membership membership) {
        String title = "Ödemeniz Onaylandı";
        String body = membership.getPlan().getName()
                + " paketiniz için yaptığınız ödeme onaylanmıştır. Üyeliğiniz aktif edilmiştir.";
        send(user, title, body);
    }

    public void sendPaymentRejected(User user, Membership membership, String reason) {
        String title = "Ödeme Talebiniz Reddedildi";
        String body = membership.getPlan().getName()
                + " paketiniz için yaptığınız ödeme talebi reddedilmiştir."
                + (reason != null ? " Gerekçe: " + reason : "");
        send(user, title, body);
    }

    public void sendCashPaymentPendingToAdmins(User user, MembershipPlan plan, Payment payment) {
        String title = "Yeni Nakit Ödeme Talebi";
        String body = user.getFullName() + " adlı üye, " + plan.getName()
                + " paketi için nakit ödeme talebinde bulunmuştur. Onayınızı beklemektedir.";
        sendToAdmins(title, body);
    }

    public void sendEftPaymentPendingToAdmins(User user, MembershipPlan plan, Payment payment) {
        String title = "Yeni EFT Dekont Bildirimi";
        String body = user.getFullName() + " adlı üye, " + plan.getName()
                + " paketi için EFT dekontunu sisteme yüklemiştir. İncelemenizi beklemektedir.";
        sendToAdmins(title, body);
    }

    public void sendSessionCancelled(User user, Session session) {
        String title = "Ders İptal Edildi";
        String body = session.getTemplate().getName() + " · "
                + session.getSessionDate() + " " + session.getStartTime()
                + " dersi iptal edilmiştir.";
        send(user, title, body);
    }

    public void sendLowSessionsWarning(User user, Membership membership) {
        String title = "Ders Hakkınız Azaldı";
        String body = membership.getPlan().getName() + " paketinizde "
                + membership.getSessionsRemaining() + " ders hakkınız kaldı.";
        send(user, title, body);
    }

    public void sendWeeklySessionsOpened(User user) {
        String title = "Yeni Haftanın Programı Açıldı";
        String body = "Önümüzdeki haftanın ders programı yayınlandı, hemen rezervasyon yapabilirsin.";
        send(user, title, body);
    }

    public void sendPlanPriceUpdated(User user, MembershipPlan plan, BigDecimal oldPrice, BigDecimal newPrice) {
        String title = "Paket Ücreti Güncellendi";
        String body = plan.getName() + " paketinin ücreti " + oldPrice
                + " ₺'den " + newPrice + " ₺'ye güncellenmiştir.";
        send(user, title, body);
    }

    public void sendTrainerPaymentMade(User trainer, BigDecimal amount) {
        String title = "Ödemeniz İşlendi";
        String body = amount + " ₺ tutarındaki ödemeniz hesabınıza işlenmiştir.";
        send(trainer, title, body);
    }

    public void sendBroadcast(User user, String title, String body) {
        send(user, title, body);
    }

    public void sendAttendanceReminder(User trainer, int incompleteCount) {
        String title = "Yoklama Hatırlatması";
        String body = "Bugün " + incompleteCount
                + " dersin yoklaması eksik. Lütfen tamamlayın.";
        send(trainer, title, body);
    }


    private void sendToAdmins(String title, String body) {
        List<User> admins = userRepository.findAllByUserTypeNameAndDeletedFalse("ADMIN");
        admins.forEach(admin -> send(admin, title, body));
    }

    private void send(User user, String title, String body) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .isRead(false)
                .build());
        log.info("[BİLDİRİM] Alıcı: {} | Başlık: {} | Mesaj: {}",
                user.getEmail(), title, body);
        fcmPushService.send(user.getFcmToken(), title, body);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return notificationRepository.findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!n.getUser().getId().equals(userId)) {
            throw new BusinessException("Bu bildirim size ait değil", HttpStatus.FORBIDDEN);
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void delete(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!n.getUser().getId().equals(userId)) {
            throw new BusinessException("Bu bildirim size ait değil", HttpStatus.FORBIDDEN);
        }
        n.setIsDeleted(true);
        notificationRepository.save(n);
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = notificationRepository.deleteAllByCreatedAtBefore(cutoff);
        log.info("Eski bildirimler temizlendi: {} kayıt silindi", deleted);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}