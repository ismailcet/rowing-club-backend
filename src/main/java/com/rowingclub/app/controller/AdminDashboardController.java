package com.rowingclub.app.controller; // TODO: admin controller'larınla aynı paketi kullan (örn. controller.admin)

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.DashboardOverviewResponse;
import com.rowingclub.app.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin · Genel Bakış (dashboard) endpoint'i.
 *
 * NOT: ApiResponse'u doğrudan döndürüyorum (diğer controller'larınla aynı stil
 * olduğunu varsaydım). ResponseEntity ile sarmalıyorsan ona çevir.
 * @PreAuthorize stilini diğer admin controller'larınla eşle; güvenlik
 * SecurityConfig içinde path bazlı yapılıyorsa bu satırı kaldırabilirsin.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> getOverview() {
        DashboardOverviewResponse data = dashboardService.getOverview();
        return ApiResponse.success(data); // TODO: factory adın farklıysa uyarla
    }
}