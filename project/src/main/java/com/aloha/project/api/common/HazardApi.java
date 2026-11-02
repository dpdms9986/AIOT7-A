package com.aloha.project.api.common;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.dto.HazardResponse;
import com.aloha.project.repository.common.HazardRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hazards")
@RequiredArgsConstructor
public class HazardApi {

    private final HazardRepository hazardRepository;

    /**
     * 관제 지도(Map)용 주의 요망(ACTIVE) 위험 구역 좌표 목록 반환
     * - 엔티티 대신 HazardResponse(DTO)로 변환해서 반환 (LAZY 필드 직렬화 에러 방지)
     */
    @GetMapping
    public ResponseEntity<List<HazardResponse>> getActiveHazards() {
        // 관제 지도에는 해소되지 않은 주의 요망(ACTIVE) 상태의 마커만 반환
        List<HazardResponse> activeHazards = hazardRepository.findByStatus("ACTIVE").stream()
                .map(HazardResponse::from)
                .toList();
        return ResponseEntity.ok(activeHazards);
    }
}