package com.aloha.mini.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Regions;

public interface RegionRepository extends JpaRepository<Regions, Long> {
    // 읍면동 이름으로 검색하되, 활성화된 지역만 반환
    List<Regions> findByEupmyeondongContainingAndIsActiveTrue(String keyword);

    Optional<Regions> findByRegionCode(String regionCode);
}
