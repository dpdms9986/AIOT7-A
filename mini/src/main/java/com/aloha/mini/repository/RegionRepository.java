package com.aloha.mini.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Regions;

public interface RegionRepository extends JpaRepository<Regions, Long> {
    // 행정구역 키워드 검색 (예: "어남동" 포함 검색 - ADM_002)
    List<Regions> findByRegionNameContaining(String keyword);

    Optional<Regions> findByRegionName(String regionName);
}
