package com.aloha.project.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 작업 배정 확정 폼 → @ModelAttribute 로 받음
 *
 * 폼에서 오는 값 예)
 *   landNos=1&landNos=5&landNos=6     → landNos = [1, 5, 6]
 *   quotas[2]=2&quotas[3]=1           → quotas  = {2=2, 3=1}  (조종사번호=배정 필지 수)
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AssignRequest {

    private List<Long> landNos = new ArrayList<>();          // 선택한 농지 번호들
    private Map<Long, Integer> quotas = new HashMap<>();     // 조종사 번호 → 배정 필지 수

    // 배정 수 합계 (get 으로 시작하지 않게 이름 지음 → 폼 바인딩 대상 아님)
    public int totalQuota() {
        return quotas.values().stream()
                .mapToInt(v -> v == null ? 0 : v)
                .sum();
    }
}