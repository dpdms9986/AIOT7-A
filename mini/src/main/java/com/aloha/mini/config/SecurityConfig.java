package com.aloha.mini.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.aloha.mini.handler.CustomAccessDeniedHandler;
import com.aloha.mini.handler.LogoutSuccessHandler;
import com.aloha.mini.service.impl.users.CustomUserDetailsServiceImpl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsServiceImpl customUserDetailsService;
    private final LogoutSuccessHandler logoutSuccessHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // REST API 세션 인증을 위해 CSRF 비활성화
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 필요 시 세션 생성
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/","/index.html", "/*.html", "/css/**", "/js/**", 
                                "/images/**", "/api/auth/**", "/api/auth/**","/api/mock/**",
                                "/login", "/logout", "/error/**", "/ws-test.html", "/ws-drone/**").permitAll() // 로그인/회원가입/중복확인은 전원 허용
                .requestMatchers("/api/admin/**").hasRole("ADMIN")   // 1. 새로운 시스템 관리자 전용

                .requestMatchers("/api/hazards", "/api/hazards/**")
                .hasAnyRole("PILOT", "LEADER")

                .requestMatchers("/api/leader/**").hasRole("LEADER") // 2. 단장 전용 (새로 추가!)
                .requestMatchers("/api/pilot/**").hasRole("PILOT")   // 3. 조종사 전용
                .anyRequest().permitAll() // 초기 개발 단계에서는 편의를 위해 전체 허용 (추후 세션 체크)
            );

            // ... 기존의 csrf, authorizeHttpRequests 등 설정 유지 ...
            // 1. 자동 로그인(Remember-Me) 설정 추가
            http
            .rememberMe(remember -> remember
                .key("drone-pass-remember-key") // 토큰 암호화용 고유 비밀키 (임의의 문자열)
                .rememberMeParameter("remember-me") // FE에서 넘겨주는 체크박스 파라미터 이름
                .tokenValiditySeconds(60 * 60 * 24 * 14) // 유지 기간: 14일 (초 단위)
                .userDetailsService(customUserDetailsService) // 사용자 정보 조회 서비스 연결
            );

        // 로그아웃 설정 및 핸들러 연결
        http.logout(logout -> logout
                .logoutUrl("/logout")                  // POST /logout 요청 시 로그아웃 처리
                .logoutSuccessHandler(logoutSuccessHandler)
                .invalidateHttpSession(true)           // 세션 초기화
                .deleteCookies("JSESSIONID", "remember-me")           // 세션 쿠키 삭제
                .permitAll()
        );

        // 인증/인가 예외 처리 핸들러 연결 (403 권한 없음 등)
        http.exceptionHandling(ex -> ex
            // 미인증 사용자가 접근 시 로그인 HTML 대신 401 JSON 반환
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\": \"UNAUTHORIZED\", \"message\": \"로그인이 필요합니다.\"}");
                })
                    .accessDeniedHandler(accessDeniedHandler));

        // UserDetailsService 등록
        http.userDetailsService(customUserDetailsService);

        return http.build();
    }
}