package com.aloha.mini.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private int status;              // HTTP 상태 코드 (예: 400, 409, 500)
    private String error;            // 에러 유형 (예: BAD_REQUEST, CONFLICT)
    private String message;          // 프론트엔드 화면에 띄울 한글 안내 메시지
    private LocalDateTime timestamp; // 에러 발생 시각
}