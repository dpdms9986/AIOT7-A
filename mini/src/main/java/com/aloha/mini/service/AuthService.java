package com.aloha.mini.service;

import com.aloha.mini.domain.PilotProfiles;
import com.aloha.mini.domain.Teams;
import com.aloha.mini.domain.UserAuth;
import com.aloha.mini.domain.Users;
import com.aloha.mini.dto.LoginRequest;
import com.aloha.mini.dto.SignUpRequest;
import com.aloha.mini.dto.UserResponse;
import com.aloha.mini.repository.PilotProfileRepository;
import com.aloha.mini.repository.TeamRepository;
import com.aloha.mini.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PilotProfileRepository pilotProfileRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. 이메일 중복 확인 (USER_002)
    public boolean checkUserIdDuplicate(String userId) {
        return userRepository.existsByUserId(userId);
    }

    // 2. 회원가입 처리 (USER_005, USER_007)
    @Transactional
    public UserResponse signUp(SignUpRequest req) {
        if (userRepository.existsByUserId(req.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일 아이디입니다.");
        }

        Teams team = null;

        

        // 단장(ADMIN)인 경우: 새 방제단 창설 및 6자리 고유 초대코드 발급
        if ("ADMIN".equalsIgnoreCase(req.getRole())) {
            String inviteCode = generateUniqueInviteCode();
            team = Teams.builder()
                    .teamName(req.getTeamName())
                    .inviteCode(inviteCode)
                    .build();
            team = teamRepository.save(team);
        } 
        // 조종사(PILOT)인 경우: 초대코드로 소속 방제단 조회 후 합류
        else if ("PILOT".equalsIgnoreCase(req.getRole())) {
            team = teamRepository.findByInviteCode(req.getInviteCode())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 방제단 초대 코드입니다."));
        }

        // 유저 기본 생성
        Users user = Users.builder()
                .userId(req.getUserId())
                .username(req.getUserName())
                .password(passwordEncoder.encode(req.getPassword())) // BCrypt 암호화
                .role(req.getRole().toUpperCase())
                .team(team)
                .enabled(1)
                .build();
                
                // 권한 객체 생성 및 추가
                UserAuth userAuth = new UserAuth();
                if ("ADMIN".equals(user.getRole())) {
                    userAuth.setAuth("ROLE_ADMIN");
                } else {
                    userAuth.setAuth("ROLE_PILOT");
                }
                user.addAuth(userAuth);

                user = userRepository.save(user);
                
        // 조종사(PILOT)인 경우: 프로필 기본 데이터 및 기본 좌표 생성 (어남동 일대 중심)
        if ("PILOT".equalsIgnoreCase(req.getRole())) {
            PilotProfiles profile = PilotProfiles.builder()
                    .user(user)
                    .skillLevel("INTERMEDIATE") // 기본 보통 숙련도
                    .phone(req.getPhone())
                    .currentLat(36.2530) // 어남동 기준 기본 초기 위치
                    .currentLng(127.4580)
                    .lastLocationUpdatedAt(LocalDateTime.now())
                    .build();
            pilotProfileRepository.save(profile);
        }

        return UserResponse.from(user);
    }

    // 3. 로그인 및 세션 등록 처리 (USER_008, USER_009)
    public UserResponse login(LoginRequest req, HttpSession session) {
        Users user = userRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일이거나 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("가입되지 않은 이메일이거나 비밀번호가 일치하지 않습니다.");
        }

        // 로그인 성공 시 세션에 필수 식별자 저장
        UserResponse response = UserResponse.from(user);
        session.setAttribute("LOGIN_USER", response);

        return response;
    }

    // 4. 로그아웃 처리 (USER_012)
    public void logout(HttpSession session) {
        session.invalidate(); // 세션 무효화
    }

    // 6자리 난수 초대코드 생성 헬퍼
    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (teamRepository.existsByInviteCode(code));
        return code;
    }

    // AuthService 클래스 내부에 추가
    public boolean checkIdDuplicate(String userId) {
        return userRepository.existsByUserId(userId);
    }
}