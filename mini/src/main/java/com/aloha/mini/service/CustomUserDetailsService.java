package com.aloha.mini.service;

import com.aloha.mini.domain.CustomUser;
import com.aloha.mini.domain.Users;
import com.aloha.mini.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userid) throws UsernameNotFoundException {
        log.info("로그인 시도 아이디 : {}", userid);

        // 1. DB에서 회원 userID(이메일)로 조회
        Users user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userid));

        // 2. CustomUser(UserDetails 구현체)로 감싸서 반환
        return new CustomUser(user);
    }
}