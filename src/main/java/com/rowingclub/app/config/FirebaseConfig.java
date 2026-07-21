package com.rowingclub.app.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FirebaseConfig {

    private static final String CREDENTIALS_PATH = "firebase-service-account.json";
    private static final List<String> FCM_SCOPES = List.of(
            "https://www.googleapis.com/auth/firebase.messaging"
    );

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(CREDENTIALS_PATH);
            if (!resource.exists()) {
                log.warn("{} bulunamadı (src/main/resources altına eklenmeli), FCM push devre dışı. "
                        + "Bildirimler yalnızca veritabanına kaydedilecek.", CREDENTIALS_PATH);
                return;
            }
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream())
                        .createScoped(FCM_SCOPES);
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