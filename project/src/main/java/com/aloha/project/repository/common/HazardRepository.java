package com.aloha.project.repository.common; // 👈 common 패키지 지정!

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.HazardReports;

public interface HazardRepository extends JpaRepository<HazardReports, Long> {
    
    // 최신 등록순으로 전체 위험 목록 조회
    List<HazardReports> findAllByOrderByCreatedAtDesc();

    // 관제 지도용: 주의 요망(ACTIVE) 상태인 위험만 조회
    List<HazardReports> findByStatus(String status);
}