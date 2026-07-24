package com.rowingclub.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_type_id", nullable = false)
    private UserType userType;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** true ise kullanıcı "silinmiş" sayılır: hiçbir listede görünmez, giriş
     *  yapamaz, geçmiş kayıtları (üyelik/ödeme/vb.) bozulmadan saklanır. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /** Antrenör yetkisi: seans detayı/üye listesini görebilir. */
    @Column(name = "can_view_roster", nullable = false)
    @Builder.Default
    private Boolean canViewRoster = true;

    /** Antrenör yetkisi: yoklama (katıldı/katılmadı) alabilir. */
    @Column(name = "can_manage_attendance", nullable = false)
    @Builder.Default
    private Boolean canManageAttendance = true;

    /** Antrenör yetkisi: "Sporcular" listesini (tüm üye rehberi) görebilir. */
    @Column(name = "can_view_athletes", nullable = false)
    @Builder.Default
    private Boolean canViewAthletes = true;

    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    @Column(name = "can_manage_daily_bookings", nullable = false)
    @Builder.Default
    private Boolean canManageDailyBookings = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userType.getName()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}