package com.aloha.mini.dto; 

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DroneLocationDto {
    private Long teamNo;       // 어느 방제단 소속인지 (같은 팀끼리만 위치를 공유하기 위함)
    private String pilotName;  // 조종사 이름 (지도 마커에 띄울 이름)
    private double latitude;   // 위도 (예: 36.3504)
    private double longitude;  // 경도 (예: 127.3845)
}