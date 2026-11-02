package com.aloha.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 작업 시작 폼 → @ModelAttribute 로 받음
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class WorkStartRequest {

    private Long droneNo;
    private Long chemicalNo;
}