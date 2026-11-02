package com.aloha.project.repository.common; // 👈 common 패키지 지정!

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.HazardReports;

public interface HazardRepository extends JpaRepository<HazardReports, Long> {
    
    // 최신 등록순으로 전체 위험 목록 조회
    // (화면에서 작성자 이름을 쓰므로 작성자(user)도 같이 가져옴 → LazyInitializationException 방지)
    @EntityGraph(attributePaths = "user")
    List<HazardReports> findAllByOrderByCreatedAtDesc();

    // 관제 지도용: 주의 요망(ACTIVE) 상태인 위험만 조회 (작성자 이름도 같이)
    @EntityGraph(attributePaths = "user")
    List<HazardReports> findByStatus(String status);
}