package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.HazardImages;

public interface HazardImageRepository extends JpaRepository<HazardImages, Long> {
    // 특정 위험 신고 게시글에 첨부된 사진 목록 조회 (BRD_006)
    List<HazardImages> findByHazardReport_No(Long hazardReportsNo);
}
