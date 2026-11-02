package com.aloha.project.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountController {

    // private final UserService userService; // 서비스 연결 필요시 주입

    // 1. 계정 찾기 페이지 이동
    @GetMapping("/find-account")
    public String findAccountPage() {
        return "page/user/find-account"; // 타임리프 뷰 경로
    }

    // 2. 아이디 찾기 처리
    @PostMapping("/find-id")
    public String findId(@RequestParam("name") String name, Model model) {
        // 아이디 찾는 로직 구현
        return "page/user/find-account";
    }

    // 3. 비밀번호 찾기 처리
    @PostMapping("/find-password")
    public String findPassword(@RequestParam("email") String email, @RequestParam("name") String name, Model model) {
        // 비밀번호 찾는 로직 구현
        return "page/user/find-account";
    }
}