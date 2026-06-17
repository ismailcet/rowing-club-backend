package com.rowingclub.app.dto;

import java.util.List;

/**
 * Admin Genel Bakış (dashboard) ekranının tek çağrıda ihtiyaç duyduğu özet.
 *
 * GET /api/admin/dashboard/overview -> ApiResponse<DashboardOverviewResponse>
 */
public record DashboardOverviewResponse(
        int activeMembers,                            // "128 Aktif üye"
        int newMembersThisWeek,                       // "+4"
        int sessionsThisWeek,                         // "34 Açılan sınıf"
        int occupancyPercent,                         // "%76"
        List<DailyOccupancyResponse> weeklyOccupancy, // Pzt..Paz çubukları
        List<AttentionItemResponse> attentionItems    // "Dikkat gerektiren"
) {
    /** Haftanın bir günü için doluluk yüzdesi (grafik çubuğu). */
    public record DailyOccupancyResponse(
            String dayLabel,   // "Pzt", "Sal", ...
            int percent        // 0..100
    ) {}

    /** Dikkat gerektiren tek bir uyarı maddesi. */
    public record AttentionItemResponse(
            String type,       // örn. "PENDING_PAYMENT"
            String message,    // örn. "3 bekleyen ödeme onayı"
            Integer count      // ilgili adet (opsiyonel)
    ) {}
}