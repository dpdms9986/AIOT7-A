package com.aloha.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 작업 시작 폼 → @ModelAttribute 로 받음
 * ※ 필드 이름 = HTML input/select 의 name
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class WorkStartRequest {
    private Long droneNo;      // name="droneNo"
    private Long chemicalNo;   // name="chemicalNo"
    private Double sprayRate;  // name="sprayRate"  (L/ha)
}