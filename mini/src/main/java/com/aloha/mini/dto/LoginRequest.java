package com.aloha.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class LoginRequest {
    private String userId;   // 로그인 이메일
    private String password; // 비밀번호
    private boolean rememberMe; // FE에서 보내는 JSON 키 이름과 맞춤 (예: rememberMe 또는 autoLogin)
}