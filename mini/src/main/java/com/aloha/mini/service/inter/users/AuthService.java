package com.aloha.mini.service.inter.users;

import com.aloha.mini.dto.LoginRequest;
import com.aloha.mini.dto.SignUpRequest;
import com.aloha.mini.dto.UserResponse;
import jakarta.servlet.http.HttpSession;

public interface AuthService {

    // 1. 이메일 중복 확인 (USER_002)
    boolean checkUserIdDuplicate(String userId);

    // 2. 회원가입 처리 (USER_005, USER_007)
    UserResponse signUp(SignUpRequest req);

    // 3. 로그인 및 세션 등록 처리 (USER_008, USER_009)
    UserResponse login(LoginRequest req, HttpSession session);

    // 4. 로그아웃 처리 (USER_012)
    void logout(HttpSession session);

    // 컨트롤러에서 쓰고 있으면 유지, 아니면 삭제
    boolean checkIdDuplicate(String userId);
}