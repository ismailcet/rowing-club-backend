-- V18: Üyelik tamamlanma mantığı değişikliği için tek seferlik veri düzeltmesi.
--
-- Eski mantıkta üyelik, son ders REZERVE EDİLDİĞİNDE (ders hakkı 0'a inince)
-- hemen COMPLETED yapılıyordu. Yeni mantıkta üyelik, son rezerve edilen dersin
-- TARİHİ GEÇENE kadar ACTIVE kalır (COMPLETED'a geçişi zamanlanmış görev yapar).
--
-- Bu yüzden hâlâ geleceğe dönük (henüz yapılmamış) aktif dersi olduğu hâlde
-- yanlışlıkla COMPLETED'a çekilmiş üyelikleri yeniden ACTIVE'e alıyoruz.
-- Tarih bazlı (>= CURRENT_DATE) ve toleranslı tutuldu; bugünden sonrası "yapılmamış"
-- sayılır. Bugün içinde gerçekten bitmiş bir ders varsa, saat başı çalışan
-- completeFinishedMemberships görevi üyeliği kısa süre içinde tekrar COMPLETED yapar.

UPDATE memberships m
SET status = 'ACTIVE'
WHERE m.status = 'COMPLETED'
  AND EXISTS (
    SELECT 1
    FROM enrollments e
             JOIN sessions s ON s.id = e.session_id
    WHERE e.membership_id = m.id
      AND e.status = 'ACTIVE'
      AND s.session_date >= CURRENT_DATE
);