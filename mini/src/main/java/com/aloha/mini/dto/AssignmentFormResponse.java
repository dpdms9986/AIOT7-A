package com.aloha.mini.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AssignmentFormResponse {
    private List<PilotInfo> pilots;
    private List<LandInfo> lands;

    // 조종사 정보 (셀렉트 박스용)
    @Getter
    @Builder
    public static class PilotInfo {
        private Long userNo;     // Users의 PK (실제 배정 시 필요)
        private String username; // 조종사 이름 (화면 표시용)
    }

    // 농지 정보 (체크박스/리스트용)
    @Getter
    @Builder
    public static class LandInfo {
        private Long landNo;     // Lands의 PK (실제 배정 시 필요)
        private String address;  // 농지 주소 (화면 표시용)
        // 필요하다면 면적, 위경도 등을 추가할 수 있습니다.
    }
}