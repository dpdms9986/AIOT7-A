package com.aloha.mini.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {
    // 변경할 상태값 (예: "IN_PROGRESS", "COMPLETED")
    private String status;
}