
package com.aloha.project.dto;

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
public class PilotAssignmentResponse {

    private Long assignmentNo; // 작업 배정 번호
    private Long landNo;       // 농지 번호
    private String address;    // 농지 주소
    private Double latitude;   // 위도
    private Double longitude;  // 경도
    private String status;     // 작업 상태 (ASSIGNED, IN_PROGRESS, COMPLETED)

}
