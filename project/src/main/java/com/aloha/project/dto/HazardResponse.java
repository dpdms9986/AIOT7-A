package com.aloha.project.dto;

import java.time.LocalDateTime;

import com.aloha.project.domain.tasks.HazardReports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 위험 구역 응답용 (GET /api/hazards)
 * - 엔티티를 그대로 JSON 으로 내보내면 LAZY 필드(user, region) 때문에 에러가 나서,
 *   지도에 필요한 값만 담아서 보냄
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HazardResponse {

    private Long no;                  // 위험 신고 번호
    private String title;             // 제목
    private String content;           // 상세 내용
    private String hazardType;        // 고위험 / 중위험 / 저위험
    private Double latitude;          // 위도
    private Double longitude;         // 경도
    private String status;            // ACTIVE / RESOLVED
    private String authorName;        // 작성자 이름
    private LocalDateTime createdAt;  // 등록 일시

    // 엔티티 → 응답 DTO
    public static HazardResponse from(HazardReports h) {
        if (h == null) return null;
        return HazardResponse.builder()
                .no(h.getNo())
                .title(h.getTitle())
                .content(h.getContent())
                .hazardType(h.getHazardType())
                .latitude(h.getLatitude())
                .longitude(h.getLongitude())
                .status(h.getStatus())
                .authorName(h.getUser() != null ? h.getUser().getName() : "-")
                .createdAt(h.getCreatedAt())
                .build();
    }
}