package com.aloha.mini.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.mini.domain.CustomUser;
import com.aloha.mini.domain.Lands;
import com.aloha.mini.dto.AssignRequest;
import com.aloha.mini.dto.DashboardResponse;
import com.aloha.mini.dto.UserResponse;
import com.aloha.mini.service.AdminService;
import com.aloha.mini.service.WorkAssignmentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leader")
@RequiredArgsConstructor
public class LeaderController {

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
        
        // 2) 단장(LEADER) 권한 확인
        UserResponse loginUser = (UserResponse) session.getAttribute("LOGIN_USER");
        if (!"LEARDER".equals(loginUser.getRole())) {
            return ResponseEntity.status(403).body("단장(LEADER) 권한이 필요합니다.");
        }

        // 3) 배정 비즈니스 로직 실행
        adminService.assignLandsToPilot(loginUser.getUserNo(), req);
        return ResponseEntity.ok("성공적으로 배정되었습니다.");
    }

    private final WorkAssignmentService workAssignmentService;

    // 단장용 방제 대시보드 조회
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal CustomUser customUser) {
        
        // 시큐리티 세션에서 현재 로그인한 단장의 소속 방제단 번호 추출
        Long teamNo = customUser.getUser().getTeam().getNo();
        
        DashboardResponse dashboardData = workAssignmentService.getDashboardData(teamNo);
        
        return ResponseEntity.ok(dashboardData);
    }
}
