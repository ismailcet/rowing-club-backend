package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Gelir listesindeki tek satır: onaylı üyelik ödemesi VEYA manuel gelir. */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class IncomeItemResponse {
    private UUID id;
    /** UYELIK (üyelik ödemesi, silinemez) veya MANUEL (silinebilir). */
    private String type;
    /** UYELIK için üyenin adı; MANUEL için kategori etiketi (Şube adı / Diğer). */
    private String title;
    /** UYELIK için plan adı + ödeme yöntemi; MANUEL için açıklama. */
    private String subtitle;
    private BigDecimal amount;
    private LocalDate date;
    /** Branş/kategori kırılım grafiği için: branş adı (birden fazlaysa " + " ile), yoksa "Diğer". */
    private String branchLabel;
}