package com.aloha.project.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class HazardRequest {
    private String title;      // 게시글 제목
    private String content;    // 상세 위험 내용
    private String hazardType; // "고위험", "중위험", "저위험"
    private Double latitude;   // 위도 좌표
    private Double longitude;  // 경도 좌표
}