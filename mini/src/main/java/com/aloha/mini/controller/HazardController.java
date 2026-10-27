
package com.aloha.mini.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.mini.domain.CustomUser;
import com.aloha.mini.dto.HazardCreateRequest;
import com.aloha.mini.dto.HazardResponse;
import com.aloha.mini.dto.HazardUpdateRequest;
import com.aloha.mini.service.HazardService;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hazards")
@RequiredArgsConstructor
public class HazardController {

    private final HazardService hazardService;

    // 위험 게시글 등록
    @PostMapping
    public ResponseEntity<HazardResponse> create(
            @Valid @RequestBody HazardCreateRequest request,
            @AuthenticationPrincipal CustomUser customUser) {

        Long userNo = customUser.getUser().getNo();

        HazardResponse response = hazardService.create(request, userNo);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 전체 위험 게시글 조회
    @GetMapping
    public ResponseEntity<List<HazardResponse>> findAll() {
        return ResponseEntity.ok(hazardService.findAll());
    }

    // 위험 게시글 상세 조회
    @GetMapping("/{no}")
    public ResponseEntity<HazardResponse> findById(
            @PathVariable("no") Long no) {

        return ResponseEntity.ok(hazardService.findById(no));
    }

    // 지역별 위험 게시글 조회
    @GetMapping("/region/{regionNo}")
    public ResponseEntity<List<HazardResponse>> findByRegion(
            @PathVariable("regionNo") Long regionNo) {

        return ResponseEntity.ok(hazardService.findByRegion(regionNo));
    }

    // 위험 게시글 수정
    @PutMapping("/{no}")
    public ResponseEntity<HazardResponse> update(
            @PathVariable("no") Long no,
            @Valid @RequestBody HazardUpdateRequest request,
            @AuthenticationPrincipal CustomUser customUser) {

        Long userNo = customUser.getUser().getNo();

        boolean isAdmin = customUser.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        HazardResponse response =
                hazardService.update(no, request, userNo, isAdmin);

        return ResponseEntity.ok(response);
    }

    // 위험 게시글 삭제
    @DeleteMapping("/{no}")
    public ResponseEntity<Void> delete(
            @PathVariable("no") Long no,
            @AuthenticationPrincipal CustomUser customUser) {

        Long userNo = customUser.getUser().getNo();

        boolean isAdmin = customUser.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        hazardService.delete(no, userNo, isAdmin);

        return ResponseEntity.noContent().build();
    }
}
