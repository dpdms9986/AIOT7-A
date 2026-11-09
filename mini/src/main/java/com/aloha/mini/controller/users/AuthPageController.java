package com.aloha.mini.controller.users;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
// 로그인·회원가입 등 인증 관련 화면을 반환하는 컨트롤러
public class AuthPageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping({"/findaccount", "/find-account"})
    public String findAccount() {
        return "findaccount";
    }
}
