package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.MaintenanceLogs;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLogs, Long> {
    
}
