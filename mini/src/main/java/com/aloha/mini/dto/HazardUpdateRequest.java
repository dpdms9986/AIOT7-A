
package com.aloha.mini.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
public class HazardUpdateRequest {

    // 게시글 제목
    private String title;

    // 게시글 내용
    private String content;

    // 위험 요소 유형
    // 위험도: 고위험, 중위험, 저위험 중 선택
    @Pattern(regexp = "고위험|중위험|저위험")
    private String hazardType;

    // 위험 요소 위치
    private Double latitude;
    private Double longitude;

    // 행정 구역 번호
    private Long regionNo;
}
