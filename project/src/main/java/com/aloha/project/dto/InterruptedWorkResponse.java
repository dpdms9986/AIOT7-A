
package com.aloha.project.dto;

import java.time.LocalDateTime;

import com.aloha.project.domain.tasks.WorkAssignments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterruptedWorkResponse {

    private Long assignmentNo;
    private Long landNo;
    private String address;
    private Double lastLatitude;
    private Double lastLongitude;
    private LocalDateTime lastLocationAt;
    private LocalDateTime interruptedAt;
    private String flightPath;
    private String message;
    private boolean canResume;

    // 작업 엔티티에서 중단 작업 팝업에 필요한 정보만 전달
    public static InterruptedWorkResponse from(WorkAssignments assignment) {
        return InterruptedWorkResponse.builder()
                .assignmentNo(assignment.getNo())
                .landNo(assignment.getLand().getNo())
                .address(assignment.getLand().getAddress())
                .lastLatitude(assignment.getLastLatitude())
                .lastLongitude(assignment.getLastLongitude())
                .lastLocationAt(assignment.getLastLocationAt())
                .interruptedAt(assignment.getInterruptedAt())
                .flightPath(assignment.getFlightPath())
                .message("작업이 강제로 중단되었습니다. 이어서 작업하시겠습니까?")
                .canResume(true)
                .build();
    }

    // 단장이 다른 조종사에게 재배정한 작업 안내
    public static InterruptedWorkResponse reassignedFrom(WorkAssignments assignment) {
        return InterruptedWorkResponse.builder()
                .assignmentNo(assignment.getNo())
                .landNo(assignment.getLand().getNo())
                .address(assignment.getLand().getAddress())
                .lastLatitude(assignment.getLastLatitude())
                .lastLongitude(assignment.getLastLongitude())
                .lastLocationAt(assignment.getLastLocationAt())
                .interruptedAt(assignment.getInterruptedAt())
                .flightPath(assignment.getFlightPath())
                .message("중단된 작업이 다른 조종사에게 배정되었습니다. 새로운 작업을 선택해 시작해주세요")
                .canResume(false)
                .build();
    }
}
