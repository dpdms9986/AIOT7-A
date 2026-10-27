
package com.aloha.mini.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HazardResponse {

    private Long no;                 // 게시글 번호
    private Long userNo;             // 작성자 회원 번호
    private String userName;         // 작성자 이름

    private String title;            // 게시글 제목
    private String content;          // 게시글 내용
    private String hazardType;       // 위험 유형

    private Double latitude;         // 위도
    private Double longitude;        // 경도
    private Long regionNo;           // 지역 번호

    private String status;            // 게시글 상태

    private LocalDateTime createdAt; // 작성일
    private LocalDateTime updatedAt; // 수정일
}
