package com.aloha.mini.controller;

import com.aloha.mini.domain.Lands;
import com.aloha.mini.dto.AssignRequest;
import com.aloha.mini.dto.UserResponse;
import com.aloha.mini.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 1. 미배정(READY) 농지 목록 조회
    // 호출 예시: GET http://localhost:8080/api/admin/lands/ready?regionNo=1
    @GetMapping("/lands/ready")
    public ResponseEntity<List<Lands>> getReadyLands(@RequestParam Long regionNo) {
        List<Lands> lands = adminService.getReadyLands(regionNo);
        return ResponseEntity.ok(lands);
    }

    // 2. 조종사에게 농지 일괄 배정
    // 호출 예시: POST http://localhost:8080/api/admin/assign
    @PostMapping("/assign")
    public ResponseEntity<String> assignLands(@RequestBody AssignRequest req, HttpServletRequest request) {
        // 1) 로그인 세션 확인
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER") == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        
        // 2) 단장(ADMIN) 권한 확인
        UserResponse loginUser = (UserResponse) session.getAttribute("LOGIN_USER");
        if (!"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(403).body("단장(ADMIN) 권한이 필요합니다.");
        }

        // 3) 배정 비즈니스 로직 실행
        adminService.assignLandsToPilot(loginUser.getUserNo(), req);
        return ResponseEntity.ok("성공적으로 배정되었습니다.");
    }
}
