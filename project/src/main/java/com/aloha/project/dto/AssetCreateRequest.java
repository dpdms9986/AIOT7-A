package com.aloha.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 자산 등록 폼 → @ModelAttribute  
 *  사용자가 값을 입력하고 버튼을 눌러서 서버로 보내는 입력 칸 
 * -> 예) 드론 자산 화면에서 +자산 등록 누르면 뜨는 팝업
 * ※ 필드 이름 = HTML input/select 의 name
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AssetCreateRequest {
    private String type;      // drone / chemical
    private String id;        // 자산 코드
    private String name;      // 이름 / 모델명
    private String subtitle;  // 설명 / 분류
    private String owner;     // 담당 조종사
    private String status;    // 상태 코드
}