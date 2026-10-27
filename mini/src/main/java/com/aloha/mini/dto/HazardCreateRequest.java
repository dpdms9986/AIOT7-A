
package com.aloha.mini.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HazardCreateRequest {

    private String title;       // 게시글 제목
    private String content;     // 게시글 내용

    // private String hazardType;  // 위험 유형
    // 위험도: 고위험, 중위험, 저위험 중 선택
    @NotBlank
    @Pattern(regexp = "고위험|중위험|저위험")
    private String hazardType;

    private Double latitude;    // 위도
    private Double longitude;   // 경도

    private Long regionNo;      // 지역 번호
}
