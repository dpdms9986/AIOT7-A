package com.aloha.project.security.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그아웃 성공 시 처리
 * - /admin/** 경로에서 로그아웃 → /login?logout (관리자 로그인)
 * - 그 외 경로에서 로그아웃 → /login?logout (일반 사용자 로그인)
 */
@Slf4j
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                 Authentication authentication) throws IOException, ServletException {
      
      String username = authentication != null ? authentication.getName() : "anonymous";
      log.info("로그아웃 성공: {}", username);
      
      // redirect 파라미터 있으면, 해당 페이지로 이동
      String redirect = request.getParameter("redirect");
      if( redirect != null && !redirect.isEmpty() ) {
        log.info("redirect 파라미터로 이동: {}", redirect);
        response.sendRedirect(redirect);
        return;
      }

      // Referer 헤더를 통해 로그아웃 요청이 어디서 왔는지 확인
      String referer = request.getHeader("Referer");
      String targetUrl = "/";
      
      // /admin 경로에서 로그아웃한 경우 관리자 로그인 페이지로
      if (referer != null && referer.contains("/admin")) {
        targetUrl = "/login?logout";
        log.info("관리자 로그아웃 - 관리자 로그인 페이지로 리다이렉트: {}", targetUrl);
      } 
      // 그 외 경로는 일반 사용자 로그인 페이지로
      else {
        targetUrl = "/login?logout";
        log.info("사용자 로그아웃 - 사용자 로그인 페이지로 리다이렉트: {}", targetUrl);
      }
      
      response.sendRedirect(targetUrl);
    }
}
