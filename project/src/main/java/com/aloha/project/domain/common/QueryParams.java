package com.aloha.project.domain.common;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryParams {
    private Integer page;                       // 현재 페이지 번호
    private Integer size;                       // 페이지당 데이터 수
    private String search;                      // 검색어
    private String sort;                        // 정렬 기준 (예: "name,createdAt")
    private List<String> sortBy;                // 정렬 기준 (예: "name", "createdAt")
    private List<String> sortOrder;             // 정렬 순서 (asc 또는 desc)
    private Integer filter;                     // 필터링 기준 (1,2,3,4)
    private Map<String, Object> params;         // 추가적인 쿼리 파라미터

    public QueryParams() {
        this.page = 1;
        this.size = 10;
        this.search = null;
        this.sortBy = null;
        this.sortOrder = null;
        this.params = new java.util.HashMap<>(); // ⭐ 추가
    }

    public QueryParams(Integer page, Integer size, String search, String sort, List<String> sortBy, List<String> sortOrder, Integer filter, Map<String, Object> params) {
        this.page = page == null ? 1 : page;
        this.size = size == null ? 10 : size;
        this.search = search;
        this.sort = sort;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.filter = filter;
        this.params = params; 
    }
    
}
