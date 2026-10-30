package com.aloha.project.dto;

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

}