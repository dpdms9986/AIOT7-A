package com.aloha.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StatusUpdateRequest {

    // 변경할 작업 상태
    private String status;

}