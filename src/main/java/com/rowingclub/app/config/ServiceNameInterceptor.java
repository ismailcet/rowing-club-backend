package com.rowingclub.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * İsteği fiilen işleyen controller sınıfı + metodunu (ör.
 * "AdminUserController.deleteUser") request attribute olarak set eder;
 * ServiceIoLogFilter bunu okuyup service_io_logs.service_name'e yazar.
 *
 * URL path'ten tahmin etmek yerine gerçek kod yolunu yakaladığı için
 * daha güvenilir — path pattern'i değişse bile doğru kalır.
 */
@Component
public class ServiceNameInterceptor implements HandlerInterceptor {

    public static final String SERVICE_NAME_ATTRIBUTE = "io_log_service_name";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            String serviceName = handlerMethod.getBeanType().getSimpleName()
                    + "." + handlerMethod.getMethod().getName();
            request.setAttribute(SERVICE_NAME_ATTRIBUTE, serviceName);
        }
        return true;
    }
}