package com.aloha.mini.service;

import com.aloha.mini.domain.Lands;
import com.aloha.mini.domain.Regions;
import com.aloha.mini.repository.LandRepository;
import com.aloha.mini.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MockDataService {

private final RegionRepository regionRepository;
private final LandRepository landRepository;

    @Transactional
    public String createMockLands() {
        // 1. 행정구역 생성 또는 조회
        Regions region = regionRepository.findByRegionName("대전시 동구 어남동")
                .orElseGet(() -> {
                    Regions newRegion = Regions.builder()
                            .regionName("대전시 동구 어남동")
                            .centerLat(36.2530)
                            .centerLng(127.4580)
                            .build();
                    return regionRepository.save(newRegion);
                });

        // 2. 어남동 주변 가상 농지 20개 생성
        List<Lands> mockLands = new ArrayList<>();
        Random random = new Random();

        // 중심 좌표에서 위경도 ±0.005 정도의 난수 발생 (약 500m 이내 반경)
        double baseLat = region.getCenterLat();
        double baseLng = region.getCenterLng();

        for (int i = 1; i <= 20; i++) {
            double randomLat = baseLat + (random.nextDouble() - 0.5) * 0.01;
            double randomLng = baseLng + (random.nextDouble() - 0.5) * 0.01;
            // 면적은 1000 ~ 5000㎡ 사이 랜덤
            double randomArea = 1000 + (random.nextDouble() * 4000);

            // 주의: 아래 빌더의 메서드명은 Lands.java 엔티티의 변수명과 100% 일치해야 합니다.
            Lands land = Lands.builder()
                    .region(region)
                    .address("대전시 동구 어남동 산" + random.nextInt(100) + "-" + i)
                    .latitude(randomLat)  // 엔티티 변수명이 latitude 인지 확인 (lat 인 경우 .lat() 사용)
                    .longitude(randomLng) // 엔티티 변수명이 longitude 인지 확인 (lng 인 경우 .lng() 사용)
                    .areaSize(Math.round(randomArea * 10.0) / 10.0) // 엔티티 변수명이 areaSize 인지 확인
                    .status("READY")
                    .build();

            mockLands.add(land);
        }

    // 3. 20개의 농지 데이터를 DB에 한 번에 저장합니다.
    landRepository.saveAll(mockLands);

    return "미양면 주변 미배정 가상 농지 20개가 성공적으로 생성되었습니다.";
}


}