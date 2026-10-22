package com.aloha.mini.dto;

import com.aloha.mini.domain.Lands;
import com.aloha.mini.domain.WorkAssignments;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PilotAssignmentResponse {
    private Long assignmentNo; // 작업 할당 번호
    private Long landNo;       // 농지 번호
    private String address;    // 농지 지번 주소
    private Double latitude;   // 위도
    private Double longitude;  // 경도
    private String status;     // 작업 상태 (ASSIGNED, IN_PROGRESS, COMPLETED)

    public static PilotAssignmentResponse from(WorkAssignments assignment) {
        Lands land = assignment.getLand();
        return PilotAssignmentResponse.builder()
                .assignmentNo(assignment.getNo())
                .landNo(land.getNo())
                .address(land.getAddress())
                .latitude(land.getLatitude())   
                .longitude(land.getLongitude()) 
                .status(assignment.getStatus())
                .build();
    }
}
