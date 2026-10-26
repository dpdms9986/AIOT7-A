
package com.aloha.mini.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.aloha.mini.dto.HazardCreateRequest;
import com.aloha.mini.dto.HazardResponse;
import com.aloha.mini.dto.HazardUpdateRequest;

@Service 
public interface HazardService {

    // 위험 게시글 등록
    HazardResponse create(HazardCreateRequest request, Long userNo);

    // 전체 위험 게시글 목록 조회
    List<HazardResponse> findAll();

    // 위험 게시글 상세 조회
    HazardResponse findById(Long no);

    // 지역별 위험 게시글 목록 조회
    List<HazardResponse> findByRegion(Long regionNo);

    // 위험 게시글 수정
    HazardResponse update(
            Long no,
            HazardUpdateRequest request,
            Long userNo,
            boolean isAdmin
    );

    // 위험 게시글 삭제
    void delete(Long no, Long userNo, boolean isAdmin);
}
