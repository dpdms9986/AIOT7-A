package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.HazardReports;

public interface HazardReportRepository extends JpaRepository<HazardReports, Long> {
    // 전체 위험 요소 게시글 최신순 조회 (BRD_004)
    List<HazardReports> findAllByOrderByCreatedAtDesc();

    // 행정구역별 또는 상태별(ACTIVE/RESOLVED) 필터링 조회 (BRD_004, BRD_005)
    List<HazardReports> findByRegion_NoOrderByCreatedAtDesc(Long regionsNo);
    List<HazardReports> findByStatusOrderByCreatedAtDesc(String status);
}
