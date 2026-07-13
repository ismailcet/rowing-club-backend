package com.rowingclub.app.repository;

import com.rowingclub.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByUserTypeName(String userTypeName);
    List<User> findAllByUserTypeNameAndCanManageAttendanceTrue(String userTypeName);

}