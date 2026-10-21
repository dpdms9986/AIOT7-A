package com.aloha.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter
@NoArgsConstructor
public class WorkActionRequest {
    private Long assignmentNo; // 작업 할당 고유 번호 (work_assignments.no)
    private Long landNo;       // 대상 농지 고유 번호 (lands.no)
}
