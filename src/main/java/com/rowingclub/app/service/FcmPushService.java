package com.rowingclub.app.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmPushService {

    public void send(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("FCM push gönderilemedi: {} ({})", e.getMessage(), e.getClass().getName());
            Throwable cause = e.getCause();
            int depth = 0;
            while (cause != null && depth < 5) {
                log.warn("  -> neden: {} ({})", cause.getMessage(), cause.getClass().getName());
                cause = cause.getCause();
                depth++;
            }
            log.warn("Tam stack trace:", e);
        }
    }
}