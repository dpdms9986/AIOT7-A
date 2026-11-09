package com.aloha.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class SignupRequest {

    private String username;     // 로그인 아이디 (이메일)
    private String password;   // 비밀번호
    private String name;   // 실명
    private String role;       // ADMIN, PILOT, LEADER

    // LEADER일 경우 필수
    private String teamName;   // 신규 방제단 명칭

    // PILOT일 경우 필수
    private String inviteCode; // 방제단 6자리 초대코드
    private String phone;      // 비상 연락처

}