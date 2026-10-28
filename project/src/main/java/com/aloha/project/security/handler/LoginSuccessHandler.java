package com.aloha.project.security.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import com.aloha.project.domain.users.CustomUser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
      log.info("로그인 성공: {}", authentication.getName());

      CustomUser customUser = (CustomUser) authentication.getPrincipal();
      HttpSession session = request.getSession();
      session.setAttribute("user", customUser.getUser());

      // 아이디 저장
      String rememberId = request.getParameter("remember-id");    // ✅ 아이디 저장 여부
      String username = request.getParameter("username");         // 👩‍💼 아이디

      // 아이디 저장 체크 ✅ 
      if( rememberId != null && rememberId.equals("on") ) {
          Cookie cookie = new Cookie("remember-id", username);     // 쿠키에 아이디 등록
          cookie.setMaxAge(60 * 60 * 24 * 7);                      // 유효기간 : 7일
          cookie.setPath("/");
          response.addCookie(cookie);
      }
      // 아이디 저장 체크 ❌ 
      else {
          Cookie cookie = new Cookie("remember-id", username);     // 쿠키에 아이디 등록
          cookie.setMaxAge(0);                                     // 유효기간 : 0 (만료)
          cookie.setPath("/");
          response.addCookie(cookie);
      }

      // redirect 파라미터 있으면, 해당 페이지로 이동
      String redirect = request.getParameter("redirect");
      if( redirect != null && !redirect.isEmpty() ) {
          log.info("redirect 파라미터로 이동: {}", redirect);
          response.sendRedirect(redirect);
          return;
      }

      // 이전에 요청했던 페이지가 있으면 해당 페이지로 리다이렉트
      SavedRequest savedRequest = requestCache.getRequest(request, response);
      if (savedRequest != null) {
          String targetUrl = savedRequest.getRedirectUrl();
          log.info("이전 요청 페이지로 리다이렉트: {}", targetUrl);
          getRedirectStrategy().sendRedirect(request, response, targetUrl);
          return;
      }

      // 권한별 대시보드 이동
      // 1. ROLE_PILOT -> /pilot/dashboard
      if (customUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PILOT"))) {
          log.info("파일럿 대시보드로 이동");
          String targetUrl = "/pilot/dashboard";
          response.sendRedirect(targetUrl);
          return;
      }

      // 2. ROLE_LEADER -> /leader/dashboard
      if (customUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LEADER"))) {
          log.info("단장 대시보드로 이동");
          String targetUrl = "/leader/dashboard";
          response.sendRedirect(targetUrl);
          return;
      }

      // 기본 페이지로 이동
      log.info("기본 페이지로 이동");
      String targetUrl = "/";
      response.sendRedirect(targetUrl);
    }
  
}
