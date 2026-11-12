package com.aloha.project.dto.common;

import lombok.Data;

@Data
public class GpsMessage {
    private Long teamNo;        // 방제단 번호
    private String pilotName;   // 조종사 이름
    private Double latitude;    // 위도
    private Double longitude;   // 경도
}