package com.aloha.project.security.handler;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증되지 않은 사용자의 접근 시 처리
 * - /admin/** 경로 → /login (관리자 로그인)
 * - 그 외 경로 → /party/intro/enter (일반 사용자 로그인)
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        String requestURI = request.getRequestURI();
        log.info("인증 필요 - 요청 URI: {}", requestURI);
        
        // /admin 경로 접근 시 관리자 로그인 페이지로
        if (requestURI.startsWith("/admin")) {
            log.info("관리자 로그인 페이지로 리다이렉트: /login");
            response.sendRedirect("/login");
        } 
        // 그 외 경로는 일반 사용자 로그인 페이지로
        else {
            log.info("사용자 로그인 페이지로 리다이렉트: /login");
            response.sendRedirect("/login");
        }
    }
}
