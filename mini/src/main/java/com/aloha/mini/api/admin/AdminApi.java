package com.aloha.mini.api.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.mini.dto.LandUpdateRequest;
import com.aloha.mini.service.inter.admin.AdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin") // ROLE_ADMIN만 접근 가능
@RequiredArgsConstructor
public class AdminApi {

    private final AdminService adminService;

    // 요구사항 정의서 확정 후 이곳에 관리자 전용 API(농지 관리, 방제단 관리 등)를 추가합니다.

    
    // 농지 주소 및 면적 수정
    @PutMapping("/lands/{landNo}")
    public ResponseEntity<String> updateLand(
        @PathVariable("landNo") Long landNo,
        @RequestBody LandUpdateRequest req) {

            adminService.updateLand(landNo, req);

            return ResponseEntity.ok("농지 정보가 수정되었습니다.");
        }

    // 농지 삭제
    @DeleteMapping("/lands/{landNo}")
    public ResponseEntity<String> deleteLand(
            @PathVariable("landNo") Long landNo) {

                adminService.deleteLand(landNo);

                return ResponseEntity.ok("농지가 삭제되었습니다.");
            }

}