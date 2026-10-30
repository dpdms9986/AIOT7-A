package com.aloha.project.api.common;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.tasks.HazardReports;
import com.aloha.project.repository.common.HazardRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hazards")
@RequiredArgsConstructor
public class HazardApi {

    private final HazardRepository hazardRepository;

    /**
     * 관제 지도(Map)용 주의 요망(ACTIVE) 위험 구역 좌표 목록 반환
     */
    @GetMapping
    public ResponseEntity<List<HazardReports>> getActiveHazards() {
        // 관제 지도에는 해소되지 않은 주의 요망(ACTIVE) 상태의 마커만 반환
        List<HazardReports> activeHazards = hazardRepository.findByStatus("ACTIVE");
        return ResponseEntity.ok(activeHazards);
    }
}