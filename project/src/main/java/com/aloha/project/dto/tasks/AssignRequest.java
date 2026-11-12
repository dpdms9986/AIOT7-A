package com.aloha.project.dto.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 작업 배정 확정 폼 → @ModelAttribute 로 받음
 *
 * [일반 배정]  landNos=1&landNos=5   quotas[2]=2&quotas[3]=1   (조종사번호=배정 필지 수)
 * [구역별 배정] pieceAssignmentsJson (구역별 담당 조종사 + 다각형),
 *              직접 그린 영역이면 drawnPolygon / drawnAddress / drawnLat / drawnLng / drawnArea 도 함께 옴
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AssignRequest {

    private List<Long> landNos = new ArrayList<>();          // 선택한 농지 번호들
    private Map<Long, Integer> quotas = new HashMap<>();     // 조종사 번호 → 배정 필지 수

    private String pieceAssignmentsJson;   // 구역별 담당 조종사 (JSON)

    private String drawnPolygon;           // 지도에서 직접 그린 영역 [[위도,경도],...] (JSON)
    private String drawnAddress;           // 직접 그린 영역 이름(주소)
    private Double drawnLat;               // 직접 그린 영역 중심 위도
    private Double drawnLng;               // 직접 그린 영역 중심 경도
    private Double drawnArea;              // 직접 그린 영역 면적(㎡)

    // 배정 수 합계 (get 으로 시작하지 않게 이름 지음 → 폼 바인딩 대상 아님)
    public int totalQuota() {
        return quotas.values().stream()
                .mapToInt(v -> v == null ? 0 : v)
                .sum();
    }
}