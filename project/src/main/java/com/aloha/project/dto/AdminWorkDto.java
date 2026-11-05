package com.aloha.project.dto;

import java.time.LocalDateTime;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.tasks.WorkAssignments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminWorkDto {

    private LocalDateTime workDate; // 완료 > 시작 > 배정 순으로 가장 최근 시각
    private String region;          // 예) 유성구 전민동
    private String pilotName;
    private String status;          // ASSIGNED / IN_PROGRESS / COMPLETED

    public String getStatusLabel() {
        if (status == null) return "-";
        return switch (status) {
            case "ASSIGNED"    -> "대기";
            case "IN_PROGRESS" -> "진행 중";
            case "COMPLETED"   -> "완료";
            default            -> status;
        };
    }

    public static AdminWorkDto from(WorkAssignments w) {
        Lands land = w.getLand();
   
        LocalDateTime date = w.getCompletedAt() != null ? w.getCompletedAt()
                : w.getStartedAt() != null ? w.getStartedAt()
                : w.getCreatedAt();

        return AdminWorkDto.builder()
                .workDate(date)
                .pilotName(w.getPilot() != null ? w.getPilot().getName() : "-")
                .status(w.getStatus())
                .build();
    }
}