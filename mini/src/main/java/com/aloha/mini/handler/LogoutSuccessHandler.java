package com.aloha.mini.handler;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그아웃 성공 처리 이벤트 핸들러
 */
@Slf4j 
@Component 
public class LogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request, 
                                HttpServletResponse response,
                                @Nullable Authentication authentication) throws IOException, ServletException {
        // log.info("로그아웃 성공 ...");
        
        // // 로그아웃 성공 후 리다이렉트할 경로 설정
        // setDefaultTargetUrl("/login?logout");
        
        // super.onLogoutSuccess(request, response, authentication);
        // LogoutSuccessHandler.java 내부
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"message\": \"로그아웃 되었습니다.\"}");
}

    }


