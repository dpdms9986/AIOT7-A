package com.aloha.project.controller.user;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.common.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // 👈 메일 발송 서비스 주입

    // 1. 계정 찾기 페이지 이동 (아이디/비번 찾기 통합 페이지)
    @GetMapping("/find-account")
    public String findAccountPage() {
        return "page/find-account"; // 👈 올바른 템플릿 경로로 수정!
    }

    // 2. 아이디 찾기 처리
    @PostMapping("/find-id")
    public String findId(@RequestParam("name") String name, 
                         @RequestParam(value = "phone", required = false) String phone,
                         Model model) {
        log.info("아이디 찾기 요청: name={}, phone={}", name, phone);

        // 실명(name)으로 회원 조회 (전화번호가 함께 넘어오면 추가 검증 가능)
        Users user = userRepository.findAll().stream()
                .filter(u -> name.equals(u.getName()))
                .filter(u -> phone == null || phone.isBlank() || phone.equals(u.getPhone()))
                .findFirst()
                .orElse(null);

        if (user != null) {
            // 이메일 마스킹 처리 (예: abc***@gmail.com) 또는 전체 이메일 전달
            model.addAttribute("foundUsername", user.getUserName());
            model.addAttribute("searchSuccess", true);
        } else {
            model.addAttribute("errorMessage", "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
        }

        model.addAttribute("activeTab", "id"); // 아이디 찾기 탭 유지
        return "page/find-account";
    }

    // 3. 비밀번호 찾기 (임시 비밀번호 생성 -> DB 암호화 반영 -> 메일 발송)
    @PostMapping("/find-password")
    public String findPassword(@RequestParam("email") String email, 
                               @RequestParam("name") String name, 
                               Model model) {
        log.info("비밀번호 재설정 요청: email={}, name={}", email, name);

        // 이메일(userName)과 실명(name)이 일치하는 회원 조회
        Users user = userRepository.findByUserName(email.trim()).orElse(null);

        if (user != null && name.equals(user.getName())) {
            // 8자리 임시 비밀번호 생성 (예: a1b2c3d4)
            String tempPassword = UUID.randomUUID().toString().substring(0, 8);

            // DB에 암호화하여 새 비밀번호 반영
            user.setPassword(passwordEncoder.encode(tempPassword));
            userRepository.save(user);
            log.info("임시 비밀번호 DB 업데이트 완료: user={}", user.getUserName());

            // 임시 비밀번호 이메일 발송
            try {
                emailService.sendTempPasswordMail(user.getUserName(), user.getName(), tempPassword);
                model.addAttribute("message", "입력하신 이메일로 임시 비밀번호가 발송되었습니다.");
                model.addAttribute("pwSuccess", true);
            } catch (Exception e) {
                log.error("메일 발송 실패: ", e);
                // 메일 서버 설정(Gmail 앱 비번 등)이 아직 안 되어 있을 때 개발용 안내
                model.addAttribute("message", "메일 발송 실패 (임시 비밀번호: " + tempPassword + ")");
            }

        } else {
            model.addAttribute("errorMessage", "이메일 또는 사용자 이름이 일치하지 않습니다.");
        }

        model.addAttribute("activeTab", "password"); // 비밀번호 찾기 탭 유지
        return "page/find-account";
    }
}