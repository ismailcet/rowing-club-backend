package com.rowingclub.app.repository;

import com.rowingclub.app.entity.ServiceIoLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceIoLogRepository extends JpaRepository<ServiceIoLog, UUID> {
}