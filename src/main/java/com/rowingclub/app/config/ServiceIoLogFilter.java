package com.rowingclub.app.config;

import com.rowingclub.app.entity.ServiceIoLog;
import com.rowingclub.app.repository.ServiceIoLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Her backend isteğini (request + response gövdesi, kim tarafından yapıldığı,
 * ne zaman geldi/döndü, başarılı mı değil mi, hata varsa nereden geldiği)
 * service_io_logs tablosuna kaydeder.
 *
 * GlobalExceptionHandler, yakaladığı her exception için request attribute
 * olarak "errorSource" set eder; bu filtre isteğin sonunda o attribute'u okur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceIoLogFilter extends OncePerRequestFilter {

    public static final String ERROR_SOURCE_ATTRIBUTE = "io_log_error_source";

    private static final int MAX_BODY_LENGTH = 8000;

    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "\"(password|newPassword|currentPassword|oldPassword|token|refreshToken|accessToken|secret)\"\\s*:\\s*\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE
    );

    private final ServiceIoLogRepository repository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request, MAX_BODY_LENGTH);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        LocalDateTime requestedAt = LocalDateTime.now();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            LocalDateTime respondedAt = LocalDateTime.now();
            try {
                persistLog(wrappedRequest, wrappedResponse, requestedAt, respondedAt);
            } catch (Exception e) {
                // Loglama asla asıl isteği bozmamalı.
                log.warn("service_io_log kaydedilemedi: {}", e.getMessage());
            }
            // KRİTİK: bu çağrı olmadan gerçek response gövdesi hiçbir zaman
            // istemciye yazılmaz (ContentCachingResponseWrapper sadece kopyalar).
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void persistLog(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            LocalDateTime requestedAt,
            LocalDateTime respondedAt
    ) {
        int statusCode = response.getStatus();
        boolean success = statusCode < 400;

        String errorSource = (String) request.getAttribute(ERROR_SOURCE_ATTRIBUTE);
        String serviceName = (String) request.getAttribute(
                com.rowingclub.app.config.ServiceNameInterceptor.SERVICE_NAME_ATTRIBUTE);

        ServiceIoLog logEntry = ServiceIoLog.builder()
                .userEmail(extractUserEmail())
                .httpMethod(request.getMethod())
                .path(buildFullPath(request))
                .serviceName(serviceName)
                .requestBody(redactAndTruncate(readBody(request.getContentAsByteArray())))
                .responseBody(redactAndTruncate(readBody(response.getContentAsByteArray())))
                .statusCode(statusCode)
                .status(success ? "SUCCESS" : "FAILURE")
                .errorSource(errorSource)
                .requestedAt(requestedAt)
                .respondedAt(respondedAt)
                .durationMs(Duration.between(requestedAt, respondedAt).toMillis())
                .build();

        repository.save(logEntry);
    }

    private String extractUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private String buildFullPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private String readBody(byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String redactAndTruncate(String body) {
        if (body == null) {
            return null;
        }
        String redacted = SENSITIVE_FIELD.matcher(body).replaceAll("\"$1\":\"***\"");
        if (redacted.length() > MAX_BODY_LENGTH) {
            return redacted.substring(0, MAX_BODY_LENGTH) + "... (kesildi)";
        }
        return redacted;
    }
}