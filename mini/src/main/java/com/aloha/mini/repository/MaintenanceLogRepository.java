package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.MaintenanceLogs;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLogs, Long> {
    // 특정 드론의 정비 이력 최신순 조회 (AST_005)
    List<MaintenanceLogs> findByDrone_NoOrderByMaintenanceDateDesc(Long dronesNo);
}
