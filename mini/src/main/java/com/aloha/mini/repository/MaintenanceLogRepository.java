package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Notices;

public interface MaintenanceLogRepository extends JpaRepository<Notices, Long> {
    
}
