package com.rowingclub.app.repository;

import com.rowingclub.app.entity.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MembershipTypeRepository extends JpaRepository<MembershipType, UUID> {
    Optional<MembershipType> findByName(String name);
}