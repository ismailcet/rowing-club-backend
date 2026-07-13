package com.rowingclub.app.service;

import com.rowingclub.app.dto.DashboardOverviewResponse;
import com.rowingclub.app.dto.DashboardOverviewResponse.AttentionItemResponse;
import com.rowingclub.app.dto.DashboardOverviewResponse.DailyOccupancyResponse;
import com.rowingclub.app.dto.PaymentResponse;
import com.rowingclub.app.dto.SessionResponse;
import com.rowingclub.app.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin dashboard özetini, halihazırda test edilmiş servisleri çağırarak üretir.
 * Yeni repository sorgusu yazmaz; mevcut DTO'lar üzerinden Java tarafında toplar.
 *
 * ============================ DÜZENLENECEKLER ============================
 * Aşağıdaki servis metod adlarını kendi projendeki gerçek imzalarla eşle.
 * Beklenen dönüşler:
 *   - userService.getAllUsers()              -> List<UserResponse>
 *   - sessionService.getAllSessions(start, end) -> List<SessionResponse>
 *   - paymentService.getPendingPayments()    -> List<PaymentResponse>
 * Ayrıca MEMBER_TYPE değerini user_types tablosundaki gerçek isimle değiştir.
 * =======================================================================
 */
@Service
public class DashboardService {

    /** user_types tablosundaki üye tipinin adı. TODO: gerçek değerle değiştir. */
    private static final String MEMBER_TYPE = "MEMBER";

    private static final String[] TR_DAYS = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};

    private final UserService userService;
    private final SessionService sessionService;
    private final PaymentService paymentService;

    public DashboardService(UserService userService,
                            SessionService sessionService,
                            PaymentService paymentService) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.paymentService = paymentService;
    }

    public DashboardOverviewResponse getOverview() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        // --- Üyeler ---
        List<UserResponse> members = userService.getAllUsers().stream()
                .filter(u -> MEMBER_TYPE.equalsIgnoreCase(u.getUserType()))
                .toList();

        int activeMembers = (int) members.stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .count();

        int newMembersThisWeek = (int) members.stream()
                .filter(u -> isInCurrentWeek(u, weekStart, weekEnd))
                .count();

        // --- Bu haftaki sessionlar ---
        List<SessionResponse> sessions = sessionService.getAllSessions(weekStart, weekEnd);
        int sessionsThisWeek = sessions.size();

        // --- Genel doluluk (Σcurrent / Σmax) ---
        int totalCurrent = sessions.stream().mapToInt(this::current).sum();
        int totalMax = sessions.stream().mapToInt(this::max).sum();
        int occupancyPercent = totalMax == 0 ? 0 : Math.round(totalCurrent * 100f / totalMax);

        // --- Günlük doluluk (Pzt..Paz) ---
        List<DailyOccupancyResponse> weeklyOccupancy = buildWeeklyOccupancy(sessions);

        // --- Dikkat gerektiren ---
        List<AttentionItemResponse> attention = buildAttentionItems();

        return new DashboardOverviewResponse(
                activeMembers,
                newMembersThisWeek,
                sessionsThisWeek,
                occupancyPercent,
                weeklyOccupancy,
                attention
        );
    }

    private List<DailyOccupancyResponse> buildWeeklyOccupancy(List<SessionResponse> sessions) {
        // gün -> [current, max]
        Map<DayOfWeek, int[]> byDay = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            byDay.put(d, new int[]{0, 0});
        }
        for (SessionResponse s : sessions) {
            if (s.getSessionDate() == null) continue;
            int[] acc = byDay.get(s.getSessionDate().getDayOfWeek());
            acc[0] += current(s);
            acc[1] += max(s);
        }
        List<DailyOccupancyResponse> result = new ArrayList<>(7);
        for (DayOfWeek d : DayOfWeek.values()) { // MONDAY..SUNDAY
            int[] acc = byDay.get(d);
            int pct = acc[1] == 0 ? 0 : Math.round(acc[0] * 100f / acc[1]);
            result.add(new DailyOccupancyResponse(TR_DAYS[d.getValue() - 1], pct));
        }
        return result;
    }

    private List<AttentionItemResponse> buildAttentionItems() {
        List<AttentionItemResponse> items = new ArrayList<>();

        // Bekleyen ödeme onayları
        List<PaymentResponse> pending = paymentService.getPendingPayments();
        if (pending != null && !pending.isEmpty()) {
            int n = pending.size();
            items.add(new AttentionItemResponse(
                    "PENDING_PAYMENT",
                    n + " bekleyen ödeme onayı",
                    n
            ));
        }

        // TODO: model genişledikçe eklenebilir:
        //  - "antrenör atanmamış sınıf"  (session/template'e trainer alanı eklenince)
        //  - "ders paketi bu hafta dolan üye" (üyelik bitiş/azalma listesi açılınca)

        return items;
    }

    /** UserResponse.createdAt'in bu hafta içinde olup olmadığı. */
    private boolean isInCurrentWeek(UserResponse u, LocalDate weekStart, LocalDate weekEnd) {
        if (u.getCreatedAt() == null) return false;
        LocalDate created = u.getCreatedAt().toLocalDate();
        return !created.isBefore(weekStart) && !created.isAfter(weekEnd);
    }

    private int current(SessionResponse s) {
        int base = s.getCurrentCapacity() == null ? 0 : s.getCurrentCapacity();
        int training = s.getCurrentTrainingCapacity() == null ? 0 : s.getCurrentTrainingCapacity();
        return base + training;
    }

    private int max(SessionResponse s) {
        int base = s.getMaxCapacity() == null ? 0 : s.getMaxCapacity();
        int training = s.getTrainingCapacity() == null ? 0 : s.getTrainingCapacity();
        return base + training;
    }
}