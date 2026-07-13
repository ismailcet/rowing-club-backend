package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bir antrenörün yetkilerini günceller. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTrainerPermissionsRequest {
    /** Üye listesi / seans detayını görebilir mi? */
    private Boolean canViewRoster;
    /** Yoklama (katıldı/katılmadı) alabilir mi? */
    private Boolean canManageAttendance;
    /** "Sporcular" listesini (tüm üye rehberi) görebilir mi? */
    private Boolean canViewAthletes;
}