package com.aloha.project.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자산 이력 한 줄 (드론 정비 이력 / 약제 입고 이력)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetLogDto {
    private LocalDate date;   // 일자
    private String kind;      // 드론: 정기 점검, 부품 교체 … / 약제: 입고 수량 (예: 20L)
    private String content;   // 내용
    private String note;      // 드론: 비용 / 약제: 비고
}