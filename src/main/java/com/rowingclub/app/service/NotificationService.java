package com.rowingclub.app.service;

import com.rowingclub.app.entity.Membership;
import com.rowingclub.app.entity.MembershipPlan;
import com.rowingclub.app.entity.Payment;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;

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


    private void sendToAdmins(String title, String body) {
        List<User> admins = userRepository.findAllByUserTypeName("ADMIN");
        admins.forEach(admin -> send(admin, title, body));
    }

    private void send(User user, String title, String body) {
        // TODO: FCM entegrasyonu buraya gelecek
        // fcmService.send(user.getFcmToken(), title, body);
        log.info("[BİLDİRİM] Alıcı: {} | Başlık: {} | Mesaj: {}",
                user.getEmail(), title, body);
    }
}