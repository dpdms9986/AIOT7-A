package com.aloha.project.api.users; // 패키지 경로 확인

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.service.inter.users.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthApi {

    private final UserService userService;

 /**
     * 이메일(아이디) 중복확인
     * - 현재 프론트엔드가 호출하는 /check-username 과
     * - 팀 규격인 /api/auth/check-email 둘 다 지원!
     */
    @GetMapping({"/check-email", "/check-username"})
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam("userName") String userName) {
        log.info("이메일 중복확인 요청: {}", userName);
        
        Users user = userService.selectByUsername(userName.trim());
        boolean isDuplicate = (user != null);

        return ResponseEntity.ok(isDuplicate);
    }

    /**
     * 2. 현재 로그인한 사용자 정보 조회 (지도 관제 및 웹소켓 연결 시 사용)
     * GET /api/auth/me
     */
    @GetMapping("/me") // 👈 백엔드 팀의 /api/auth/api/auth/me URL 오타 수정!
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal CustomUser customUser) {
        // 로그인하지 않은 상태면 401 응답
        if (customUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        Users loginUser = customUser.getUser();

        // 프론트엔드 관제 화면에 필요한 핵심 정보만 JSON으로 반환
        return ResponseEntity.ok(Map.of(
            "no", loginUser.getNo(),
            "username", loginUser.getUserName(),
            "name", loginUser.getName(),
            "role", loginUser.getRole(),
            "teamName", loginUser.getTeam() != null ? loginUser.getTeam().getTeamName() : ""
        ));
    }
}