package com.aloha.project.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.Users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 조종사 관리 - 조종사 한 명 (작업 집계 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPilotDto {

    private Long no;
    private String name;
    private String userName;        // 아이디(이메일)
    private String phone;
    private Long teamNo;
    private String teamName;
    private boolean enabled;        // 계정 활성 여부
    private LocalDateTime createdAt;

    private long assignedCount;     // 대기(배정됨)
    private long inProgressCount;   // 작업 중
    private long completedCount;    // 완료
    private LocalDateTime lastWorkAt; // 가장 최근 작업 시각

    /** WORKING(작업 중) / READY(대기) / DISABLED(비활성) */
    public String getStatus() {
        if (!enabled) return "DISABLED";
        return inProgressCount > 0 ? "WORKING" : "READY";
    }

    public String getStatusLabel() {
        return switch (getStatus()) {
            case "DISABLED" -> "비활성";
            case "WORKING"  -> "작업 중";
            default         -> "대기";
        };
    }

    // style.css 의 asset-pilot-badge 색: low(빨강) / operating(파랑) / normal(초록)
    public String getStatusClass() {
        return switch (getStatus()) {
            case "DISABLED" -> "low";
            case "WORKING"  -> "operating";
            default         -> "normal";
        };
    }

    public static AdminPilotDto of(Users u, List<WorkAssignments> works) {
        LocalDateTime last = works.stream()
                .map(w -> w.getCompletedAt() != null ? w.getCompletedAt()
                        : w.getStartedAt() != null ? w.getStartedAt() : w.getCreatedAt())
                .filter(d -> d != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return AdminPilotDto.builder()
                .no(u.getNo())
                .name(u.getName())
                .userName(u.getUserName())
                .phone(u.getPhone() != null ? u.getPhone() : "-")
                .teamNo(u.getTeam() != null ? u.getTeam().getNo() : null)
                .teamName(u.getTeam() != null ? u.getTeam().getTeamName() : "-")
                .enabled(Boolean.TRUE.equals(u.getEnabled()))
                .createdAt(u.getCreatedAt())
                .assignedCount(works.stream().filter(w -> "ASSIGNED".equals(w.getStatus())).count())
                .inProgressCount(works.stream().filter(w -> "IN_PROGRESS".equals(w.getStatus())).count())
                .completedCount(works.stream().filter(w -> "COMPLETED".equals(w.getStatus())).count())
                .lastWorkAt(last)
                .build();
    }
}