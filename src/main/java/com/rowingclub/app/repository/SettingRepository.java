package com.rowingclub.app.repository;

import com.rowingclub.app.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettingRepository extends JpaRepository<Setting, UUID> {
    Optional<Setting> findByKey(String key);
}