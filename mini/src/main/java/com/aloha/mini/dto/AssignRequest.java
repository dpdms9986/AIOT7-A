package com.aloha.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AssignRequest {
    private String batchTitle;  // 작업 묶음 이름 (예: "어남동 1차 공동방제")
    private Long teamNo;        // 소속 방제단 고유 번호
    private List<Long> landNos; // 체크박스로 선택한 농지 번호(PK) 목록
    private Long pilotNo;       // 배정받을 조종사 고유 번호(PK)
}
