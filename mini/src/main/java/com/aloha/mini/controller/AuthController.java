package com.aloha.mini.controller;

import com.aloha.mini.dto.LoginRequest;
import com.aloha.mini.dto.SignUpRequest;
import com.aloha.mini.dto.UserResponse;
import com.aloha.mini.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. 이메일 중복 확인 (USER_002)
    // GET /api/auth/check-email?userId=test@test.com
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkUserIdDuplicate(@RequestParam String userId) {
        boolean isDuplicate = authService.checkUserIdDuplicate(userId);
        return ResponseEntity.ok(isDuplicate);
    }

    // 2. 회원가입 (USER_007)
    // POST /api/auth/signup
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@RequestBody SignUpRequest req) {
        UserResponse response = authService.signUp(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 3. 로그인 및 세션 생성 (USER_008, USER_009)
    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        // request.getSession(true) : 기존 세션이 있으면 반환, 없으면 새로 생성
        HttpSession session = request.getSession(true);
        UserResponse response = authService.login(req, session);
        return ResponseEntity.ok(response);
    }

    // 4. 로그아웃 (USER_012)
    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        // request.getSession(false) : 세션이 있으면 반환, 없으면 null 반환 (새로 만들지 않음)
        HttpSession session = request.getSession(false);
        if (session != null) {
            authService.logout(session);
        }
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
    
    // 5. 로그인 상태 체크 (프론트엔드 새로고침 시 세션 유지 확인용)
    // GET /api/auth/status
    @GetMapping("/status")
    public ResponseEntity<UserResponse> checkStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("LOGIN_USER") != null) {
            UserResponse user = (UserResponse) session.getAttribute("LOGIN_USER");
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}