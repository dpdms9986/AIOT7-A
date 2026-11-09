package com.aloha.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 셀렉트박스 옵션 하나 (드론, 약제 목록 등)
 *   <option th:value="${o.id}" th:text="${o.label}">
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OptionDto {
    private Long id;       // 서버로 보낼 값
    private String label;  // 화면에 보이는 글자
}