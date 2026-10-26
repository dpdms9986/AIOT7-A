package com.aloha.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter
@NoArgsConstructor
public class SignUpRequest {
    private String userId;     // 로그인 아이디 (이메일)
    private String password;   // 비밀번호
    private String userName;   // 실명
    private String role;       // ADMIN 또는 PILOT
    
    // LEADER일 경우 필수
    private String teamName;   // 신규 방제단 명칭
    
    // PILOT일 경우 필수
    private String inviteCode; // 방제단 6자리 초대코드
    private String phone;      // 비상 연락처
}