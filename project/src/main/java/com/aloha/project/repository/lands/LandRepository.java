package com.aloha.project.repository.lands;

import org.springframework.data.jpa.repository.JpaRepository;
import com.aloha.project.domain.lands.*;
import java.util.List;

public interface LandRepository extends JpaRepository<Lands, Long> {
    // 상태별 농지 조회 (READY, ASSIGNED 등)
    List<Lands> findByStatus(String status);
}
