package com.aloha.project.repository.common;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.common.HazardImages;
import com.aloha.project.domain.common.HazardReports;

public interface  HazardImageRepository extends JpaRepository<HazardImages, Long> {
    // 특정 위험글에 등록된 사진 목록 조회
    List<HazardImages> findByHazard(HazardReports hazard);
    
    // 특정 위험글의 사진 전부 삭제
    void deleteByHazard(HazardReports hazard);
}