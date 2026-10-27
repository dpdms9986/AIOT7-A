
package com.aloha.mini.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HazardCreateRequest {

    private String title;       // 게시글 제목
    private String content;     // 게시글 내용
    private String hazardType;  // 위험 유형

    private Double latitude;    // 위도
    private Double longitude;   // 경도

    private Long regionNo;      // 지역 번호
}
