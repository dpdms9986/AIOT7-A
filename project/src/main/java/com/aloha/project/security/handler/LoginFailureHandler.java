package com.aloha.project.security.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

  // private RequestCache requestCache = new HttpSessionRequestCache();

  @Override
   public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
      log.info("onAuthenticationFailure");
      log.info("onAuthenticationFailure exception " + exception);

      // 실패 이유를 문장으로 만들어서 주소에 붙임 → login.html 의 $alert 가 그대로 표시
      String reason;
      if (exception instanceof BadCredentialsException) {
          reason = "아이디(이메일) 또는 비밀번호가 일치하지 않습니다.";
      } else if (exception instanceof DisabledException) {
          reason = "비활성화된 계정입니다. 방제단장에게 문의하세요.";
      } else if (exception instanceof LockedException) {
          reason = "잠긴 계정입니다. 관리자에게 문의하세요.";
      } else {
          reason = "로그인 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
      }
      String targetUrl = "/login?error=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);

      log.info("Login Failure targetUrl = " + targetUrl);

      response.sendRedirect(targetUrl);
   }
  
}
