package com.aloha.project.api.users;

import java.util.Map;
import java.util.Random;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.service.inter.common.EmailService;
import com.aloha.project.service.inter.users.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthApi {

    private final UserService userService;
    private final EmailService emailService; // 👈 1. 이메일 서비스 주입

    /**
     * 1. 이메일(아이디) 중복확인
     */
    @GetMapping({"/check-email", "/check-username", "/api/auth/check-email"})
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam("userName") String userName) {
        log.info("이메일 중복확인 요청: {}", userName);
        Users user = userService.selectByUsername(userName.trim());
        return ResponseEntity.ok(user != null);
    }

    /**
     * 2. [신규] 이메일 인증번호 발송 API
     * POST /api/auth/email/send-code?email=xxx@xxx.com
     */
    @PostMapping({"/api/auth/email/send-code", "/email/send-code"})
    public ResponseEntity<?> sendVerificationCode(@RequestParam("email") String email, 
                                                  HttpSession session) {
        String targetEmail = email.trim();
        log.info("📧 이메일 인증번호 발송 요청: {}", targetEmail);

        // ① 중복 가입 1차 방어
        Users existingUser = userService.selectByUsername(targetEmail);
        if (existingUser != null) {
            return ResponseEntity.badRequest().body("이미 가입된 이메일 주소입니다.");
        }

        // ② 6자리 인증 난수 생성 (100000 ~ 999999)
        String code = String.format("%06d", new Random().nextInt(1000000));

        // ③ 세션에 인증코드 및 3분 만료시간 저장
        long expireTime = System.currentTimeMillis() + (180 * 1000); // 3분 뒤
        session.setAttribute("AUTH_CODE_" + targetEmail, code);
        session.setAttribute("AUTH_EXPIRE_" + targetEmail, expireTime);

        // 💡 [개발용 꿀팁] 콘솔창에 찍히므로 메일함 안 열어보고도 바로 확인 가능!
        log.info("=================================================");
        log.info("👉 [{}] 인증코드: {}", targetEmail, code);
        log.info("=================================================");

        // ④ 실제 이메일 전송 (비동기 메일 발송 실패 시 예외 처리)
        try {
            emailService.sendVerificationCodeMail(targetEmail, code);
            return ResponseEntity.ok("인증번호가 발송되었습니다. 메일함을 확인해 주세요.");
        } catch (Exception e) {
            log.error("메일 발송 에러: ", e);
            return ResponseEntity.status(500).body("메일 발송에 실패했습니다. 관리자에게 문의하세요.");
        }
    }

    /**
     * 3. [신규] 이메일 인증번호 확인 API
     * POST /api/auth/email/verify-code?email=xxx@xxx.com&code=123456
     */
    @PostMapping({"/api/auth/email/verify-code", "/email/verify-code"})
    public ResponseEntity<?> verifyCode(@RequestParam("email") String email,
                                        @RequestParam("code") String code,
                                        HttpSession session) {
        String targetEmail = email.trim();
        String inputCode = code.trim();
        log.info("🔐 이메일 인증 확인 요청: email={}, code={}", targetEmail, inputCode);

        String savedCode = (String) session.getAttribute("AUTH_CODE_" + targetEmail);
        Long expireTime = (Long) session.getAttribute("AUTH_EXPIRE_" + targetEmail);

        // ① 발송 이력 확인
        if (savedCode == null || expireTime == null) {
            return ResponseEntity.badRequest().body("인증번호를 먼저 요청해 주세요.");
        }

        // ② 만료시간 체크 (3분)
        if (System.currentTimeMillis() > expireTime) {
            session.removeAttribute("AUTH_CODE_" + targetEmail);
            session.removeAttribute("AUTH_EXPIRE_" + targetEmail);
            return ResponseEntity.badRequest().body("인증번호 유효시간(3분)이 만료되었습니다. 다시 요청해 주세요.");
        }

        // ③ 일치 여부 확인
        if (!savedCode.equals(inputCode)) {
            return ResponseEntity.badRequest().body("인증번호가 일치하지 않습니다. 다시 확인해 주세요.");
        }

        // ④ 인증 성공: 세션에 "인증 완료된 이메일" 도장 쾅!
        session.setAttribute("VERIFIED_EMAIL", targetEmail);
        session.removeAttribute("AUTH_CODE_" + targetEmail);
        session.removeAttribute("AUTH_EXPIRE_" + targetEmail);

        log.info("✅ 이메일 인증 성공 도장 완료: {}", targetEmail);
        return ResponseEntity.ok(true);
    }

    /**
     * 4. 현재 로그인한 사용자 정보 조회
     */
    @GetMapping({"/me", "/api/auth/me"})
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        Users loginUser = customUser.getUser();
        return ResponseEntity.ok(Map.of(
            "no", loginUser.getNo(),
            "username", loginUser.getUserName(),
            "name", loginUser.getName(),
            "role", loginUser.getRole(),
            "teamName", loginUser.getTeam() != null ? loginUser.getTeam().getTeamName() : ""
        ));
    }
}