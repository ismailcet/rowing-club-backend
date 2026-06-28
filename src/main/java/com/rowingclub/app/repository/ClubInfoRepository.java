package com.rowingclub.app.repository;

import com.rowingclub.app.entity.ClubInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClubInfoRepository extends JpaRepository<ClubInfo, UUID> {
}