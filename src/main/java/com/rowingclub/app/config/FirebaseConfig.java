package com.rowingclub.app.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FirebaseConfig {

    @Value("${FIREBASE_CREDENTIALS_JSON:}")
    private String credentialsJson;

    @PostConstruct
    public void init() {
        if (credentialsJson == null || credentialsJson.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS_JSON tanımlı değil, FCM push devre dışı. "
                    + "Bildirimler yalnızca veritabanına kaydedilecek.");
            return;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK başlatıldı, FCM push aktif.");
            }
        } catch (Exception e) {
            log.error("Firebase Admin SDK başlatılamadı: {}", e.getMessage());
        }
    }
}