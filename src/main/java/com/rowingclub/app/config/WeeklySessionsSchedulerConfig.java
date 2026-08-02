package com.rowingclub.app.config;

import com.rowingclub.app.common.SettingKeys;
import com.rowingclub.app.service.SessionService;
import com.rowingclub.app.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.time.ZoneId;

/**
 * Haftalık ders programı oluşturma işini, sabit bir cron yerine
 * "settings" tablosundaki WEEKLY_SESSIONS_CRON_TIME değerine göre
 * çalıştırır — admin ayarı değiştirdiğinde sunucu yeniden başlatılmadan
 * bir sonraki tetiklemede yeni saat geçerli olur (her Pazar).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeeklySessionsSchedulerConfig implements SchedulingConfigurer {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final String DEFAULT_TIME = "21:30";

    private final SessionService sessionService;
    private final SettingService settingService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(
                sessionService::createWeeklySessions,
                triggerContext -> new CronTrigger(buildCronExpression(), ZONE)
                        .nextExecution(triggerContext)
        );
    }

    private String buildCronExpression() {
        String time = readTime();
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return String.format("0 %d %d * * SUN", minute, hour);
    }

    /** Ayar bulunamaz/bozuksa varsayılan saate (21:30) düşer, iş asla durmaz. */
    private String readTime() {
        try {
            String value = settingService.getStringValue(SettingKeys.WEEKLY_SESSIONS_CRON_TIME);
            String[] parts = value.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h >= 0 && h <= 23 && m >= 0 && m <= 59) {
                return value;
            }
        } catch (Exception e) {
            log.warn("WEEKLY_SESSIONS_CRON_TIME ayarı okunamadı, varsayılana ({}) düşüldü: {}",
                    DEFAULT_TIME, e.getMessage());
        }
        return DEFAULT_TIME;
    }
}