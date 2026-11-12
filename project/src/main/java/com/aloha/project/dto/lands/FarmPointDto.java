package com.aloha.project.dto.lands;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.aloha.project.dto.tasks.PilotTaskDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관제 대시보드 지도에 찍을 농지 점 하나 (JS 로 넘기는 용도)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FarmPointDto {
    private Long no;
    private String address;
    private Double area;
    private String status;
    private String statusLabel;
    private String color;
    private Double lat;
    private Double lng;
    private String pilotName;
    private String assignedAt;   // "2026-10-29 09:30" 같은 글자 (없으면 null)
    private String polygonCoordinates;

    // 농지에 배정된 조종사별 작업 및 비행 경로 정보
    private List<PilotTaskDto> pilotAssignments;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static FarmPointDto from(LandDto l) {
        return new FarmPointDto(
                l.getNo(), l.getAddress(), l.getAreaSize(),
                l.getStatus(), l.getStatusLabel(), l.getStColor(),
                l.getLatitude(), l.getLongitude(), l.getPilotName(),
                l.getAssignedAt() == null ? null : l.getAssignedAt().format(DT),
                l.getPolygonCoordinates(),
                l.getPilotAssignments()
        );
    }
}