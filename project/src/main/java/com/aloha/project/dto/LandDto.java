package com.aloha.project.dto;

import java.time.LocalDateTime;

import com.aloha.project.domain.lands.Lands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 농지 한 건 (농지 관리, 작업 배정, 조종사 대시보드에서 같이 사용)
 * ※ 백엔드 약속: 서비스는 List<LandDto> 로 넘겨준다
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandDto {

    private Long no;                  // 농지 번호 (lands.no)
    private String city;              // 시/도
    private String region;            // 시/군/구
    private String dong;              // 읍/면/동
    private String address;           // 상세 주소
    private Double latitude;          // 위도 (없으면 null)
    private Double longitude;         // 경도 (없으면 null)
    private Double areaSize;          // 면적 ㎡
    private String status;            // READY / ASSIGNED / IN_PROGRESS / COMPLETED
    private String pilotName;         // 배정 조종사 (없으면 null)
    private LocalDateTime assignedAt; // 배정 일시 (없으면 null)

    public static LandDto from(Lands land) {
        if (land == null) return null;

        return LandDto.builder()
                .no(land.getNo())
                .address(land.getAddress())
                .latitude(land.getLatitude())
                .longitude(land.getLongitude())
                .areaSize(land.getAreaSize())
                .status(land.getStatus())
                .build();
    }

    /* ===== 화면용 getter ===== */

    // 한글 상태
    public String getStatusLabel() {
        if (status == null) return "-";
        return switch (status) {
            case "READY"       -> "대기";
            case "ASSIGNED"    -> "배정됨";
            case "IN_PROGRESS" -> "작업 중";
            case "COMPLETED"   -> "완료";
            default            -> status;
        };
    }

    // 단장 농지 관리 배지 색: style.css 의 status-*
    public String getStatusClass() {
        if (status == null) return "";
        return switch (status) {
            case "READY"       -> "status-ready";
            case "ASSIGNED"    -> "status-assigned";
            case "IN_PROGRESS" -> "status-progress";
            case "COMPLETED"   -> "status-completed";
            default            -> "";
        };
    }

    // 조종사 화면 / 작업 배정 배지 색: style.css 의 st-*
    public String getStClass() {
        if (status == null) return "";
        return switch (status) {
            case "READY"       -> "st-ready";
            case "ASSIGNED"    -> "st-assigned";
            case "IN_PROGRESS" -> "st-progress";
            case "COMPLETED"   -> "st-completed";
            default            -> "";
        };
    }

    // 지도 마커 색 (대시보드 범례와 같은 색)
    public String getStColor() {
        if (status == null) return "#8a8f8c";
        return switch (status) {
            case "READY"       -> "#238451";
            case "ASSIGNED"    -> "#f2b705";
            case "IN_PROGRESS" -> "#2f80ed";
            default            -> "#8a8f8c";
        };
    }

    public String getFullRegion() {
        return String.join(" ", city, region, dong);
    }

    public boolean isDeletable() {
        return "READY".equals(status);
    }

    public boolean isCancellable() {
        return "ASSIGNED".equals(status);
    }
}