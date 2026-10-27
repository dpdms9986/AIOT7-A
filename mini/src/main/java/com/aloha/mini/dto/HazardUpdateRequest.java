
package com.aloha.mini.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HazardUpdateRequest {

    // 게시글 제목
    private String title;

    // 게시글 내용
    private String content;

    // 위험 요소 유형
    private String hazardType;

    // 위험 요소 위치
    private Double latitude;
    private Double longitude;

    // 행정 구역 번호
    private Long regionNo;
}
