package com.aloha.project.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TaskAssignRequest {
    private String batchTitle; // 배정 명칭 (예: "유성구 1차 방제")
    private Long landNo;        // 대상 농지 PK
    private Long pilotNo;       // 담당 조종사 PK
}