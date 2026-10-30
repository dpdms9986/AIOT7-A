package com.aloha.mini.api.common;

import com.aloha.mini.domain.Regions;
import com.aloha.mini.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionApi {

    private final RegionRepository regionRepository;

    @GetMapping
    public ResponseEntity<List<Regions>> searchRegions(@RequestParam(name = "keyword", required = false) String keyword) {
        // 검색어가 없거나 공백일 경우 빈 목록 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // Repository에서 검색어 기준 목록 조회
        List<Regions> regions = regionRepository.findByEupmyeondongContainingAndIsActiveTrue(keyword.trim());
        return ResponseEntity.ok(regions);
    }
}