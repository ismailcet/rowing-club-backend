package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Bir branş (üyelik tipi) için üyenin gelişim/seviye bilgisi. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchProgressResponse {

    private UUID membershipTypeId;
    private String membershipTypeName;

    /** Bu branşta "katıldı" işaretli ders sayısı. */
    private int attendedCount;

    private int levelNumber;
    private String levelName;
    private int maxLevel;

    /** Sonraki seviye adı; en üst seviyede null. */
    private String nextLevelName;
    /** Sonraki seviyeye kalan ders; en üst seviyede null. */
    private Integer neededForNext;

    /** Mevcut seviyenin alt eşiği (ilerleme çubuğu için). */
    private int currentThreshold;
    /** Sonraki seviyenin eşiği; en üst seviyede null. */
    private Integer nextThreshold;

    /** Bu branşta üye aktif bir eğitim paketinde mi? (true ise levelName = "Eğitim N".) */
    private boolean isTraining;
}