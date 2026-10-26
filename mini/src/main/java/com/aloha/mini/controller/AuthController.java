package com.aloha.mini.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.mini.dto.LoginRequest;
import com.aloha.mini.dto.SignUpRequest;
import com.aloha.mini.dto.UserResponse;
import com.aloha.mini.service.AuthService;
import com.aloha.mini.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@RestController
@Slf4j 
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CustomUserDetailsService customUserDetailsService;

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
        log.info("📌 회원가입 요청 들어옴! 이메일: {}", req.getUserId()); // 👈 이 줄 추가
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 3. 로그인 및 세션 생성 (USER_008, USER_009)
    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest req, HttpServletRequest request) {

        // request.getSession(true) : 기존 세션이 있으면 반환, 없으면 새로 생성
        HttpSession session = request.getSession(true);
        UserResponse response = authService.login(req, session);

        // 👇 [여기서부터 추가되는 세션 유지 코드] 👇
        // 3. DB에서 유저 정보와 권한(ROLE_PILOT 등)을 시큐리티 규격(UserDetails)으로 불러옴
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(req.getUserId());

        // 4. 시큐리티 인증 도장(Authentication) 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        // 5. 시큐리티 장부(SecurityContext)에 인증 도장 등록
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 6. 서버 세션(HttpSession)에 장부를 저장 (이 줄이 있어야 다음 요청에서도 로그인 상태가 유지됨)
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        // 👆 [여기까지 추가] 👆

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

    @GetMapping("/check-id")
    public ResponseEntity<?> checkIdDuplicate(@RequestParam("userId") String userId) {
    boolean isDuplicate = authService.checkIdDuplicate(userId);
    
    // 중복이면 true, 사용 가능하면 false를 JSON으로 응답
    return ResponseEntity.ok(Map.of(
            "isDuplicate", isDuplicate,
            "message", isDuplicate ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다."
    ));
}

// 예시: HttpSession 또는 SecurityContext에서 로그인 유저 정보를 꺼내 반환하는 API
    @GetMapping("/api/auth/me")
    public ResponseEntity<?> getMyInfo(HttpSession session) {
    // 1. 세션에서 로그인 시 저장해둔 유저 정보(또는 DTO)를 꺼냅니다.
    UserResponse loginUser = (UserResponse) session.getAttribute("LOGIN_USER");

    // 2. 로그인이 안 된 상태로 접근했다면 401(권한 없음) 에러를 보냅니다.
    if (loginUser == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
    }

    // 3. 로그인된 상태라면 유저 정보(userNo, userName, role, teamNo, inviteCode 등)를 반환합니다.
    return ResponseEntity.ok(loginUser);
}

// 🌻 FE 팀은 leader.html이나 pilot.html이 열릴 때 $.get('/api/auth/me')를 호출하기만 하면, 
//    누가 로그인했는지 바로 알아내어 웹소켓 연결까지 자동으로 실행할 수 있고, 로그인 안 한 사람이 주소창에 치고 들어오면 login.html로 쫓아낼 수 있습니다.
}