package com.aloha.mini.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {
    private long totalCount;      // 전체 할당 건수
    private long assignedCount;   // 대기 중 (ASSIGNED)
    private long inProgressCount; // 진행 중 (IN_PROGRESS)
    private long completedCount;  // 완료됨 (COMPLETED)
}