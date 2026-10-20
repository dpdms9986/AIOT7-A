package com.aloha.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class LoginRequest {
    private String userId;   // 로그인 이메일
    private String password; // 비밀번호
}