package com.aloha.project.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 작업(배정) 한 건 - 조종사 작업 이력, 단장 작업 이력에서 같이 사용
 * ※ 백엔드 약속: 서비스는 List<PilotTaskDto> 로 넘겨준다
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PilotTaskDto {

    private Long no;                   // 작업(배정) 번호
    private String address;            // 농지 주소
    private String region;             // 행정구역 예) 유성구 전민동  (단장 화면용)
    private Double areaSize;           // 면적 ㎡
    private String status;             // ASSIGNED / IN_PROGRESS / COMPLETED
    private String pilotName;          // 담당 조종사               (단장 화면용)
    private LocalDateTime assignedAt;  // 배정 일시

    // 작업 시작 때 채워짐
    private String droneName;          // 사용 드론
    private String chemicalName;       // 사용 약제
    private Double sprayRate;          // 살포량 (L/ha)
    private LocalDateTime startedAt;   // 시작 일시

    // 완료 보고 때 채워짐
    private Double usedAmount;         // 실제 약제 사용량 (L)
    private LocalDateTime completedAt; // 완료 일시

    /* ===== 화면용 getter ===== */

    public String getStatusLabel() {
        if (status == null) return "-";
        return switch (status) {
            case "ASSIGNED"    -> "대기";
            case "IN_PROGRESS" -> "진행 중";
            case "COMPLETED"   -> "완료";
            default            -> status;
        };
    }

    public String getStClass() {
        if (status == null) return "";
        return switch (status) {
            case "ASSIGNED"    -> "st-assigned";
            case "IN_PROGRESS" -> "st-progress";
            case "COMPLETED"   -> "st-completed";
            default            -> "";
        };
    }

    public boolean isDone() {
        return "COMPLETED".equals(status);
    }
}