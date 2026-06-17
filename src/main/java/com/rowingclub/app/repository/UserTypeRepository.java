package com.rowingclub.app.repository;

import com.rowingclub.app.entity.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserTypeRepository extends JpaRepository<UserType, UUID> {
    Optional<UserType> findByName(String name);
}