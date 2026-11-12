package com.aloha.project.dto.tasks;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkActionRequest {

    // 작업 배정 번호
    private Long assignmentNo;

    // 농지 번호
    private Long landNo;

    // 작업 시작 시 선택한 드론 번호
    private Long droneNo;
    
    // 작업 시작 시 선택한 약제 번호
    private Long chemicalNo;

}