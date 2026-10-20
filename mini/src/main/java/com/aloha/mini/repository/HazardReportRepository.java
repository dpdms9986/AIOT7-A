package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.HazardReports;

public interface HazardReportRepository extends JpaRepository<HazardReports, Long> {
    
}
