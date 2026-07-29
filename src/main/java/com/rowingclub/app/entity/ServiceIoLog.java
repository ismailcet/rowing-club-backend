package com.rowingclub.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_io_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceIoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** İsteği yapan kullanıcının e-postası — giriş yapılmamışsa null. */
    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 500)
    private String path;

    /** İsteği fiilen işleyen controller.metod, ör. "AdminUserController.deleteUser". */
    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status_code")
    private Integer statusCode;

    /** SUCCESS / FAILURE */
    @Column(nullable = false, length = 20)
    private String status;

    /** Hata varsa: hangi sınıf/satırdan kaynaklandığı + exception tipi. */
    @Column(name = "error_source", length = 1000)
    private String errorSource;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "duration_ms")
    private Long durationMs;
}