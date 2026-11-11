package com.aloha.project.security.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.service.impl.tasks.WorkAssignmentInterruptionServiceImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그아웃 성공 시 처리
 * - 조종사가 로그아웃하면 진행 중인 작업을 강제 중단 처리
 * - /admin/** 경로에서 로그아웃 → /login?logout
 * - 그 외 경로에서 로그아웃 → /login?logout
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final WorkAssignmentInterruptionServiceImpl interruptionService;

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        String username = authentication != null
                ? authentication.getName()
                : "anonymous";

        log.info("로그아웃 성공: {}", username);

        // 조종사 로그아웃 시 진행 중인 작업 강제 중단
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUser customUser) {

            Users user = customUser.getUser();

            if (user != null && "PILOT".equals(user.getRole())) {

                try {
                    interruptionService.interruptInProgressAssignments(
                            user.getNo(),
                            "PILOT_LOGOUT"
                    );
                } catch (Exception e) {
                    log.error(
                            "[로그아웃 작업 중단 처리 실패] 조종사 번호={}",
                            user.getNo(),
                            e
                    );
                }
            }
        }

        // redirect 파라미터 있으면 해당 페이지로 이동
        String redirect = request.getParameter("redirect");

        if (redirect != null && !redirect.isEmpty()) {
            log.info("redirect 파라미터로 이동: {}", redirect);
            response.sendRedirect(redirect);
            return;
        }

        // Referer 헤더를 통해 로그아웃 요청이 어디서 왔는지 확인
        String referer = request.getHeader("Referer");
        String targetUrl = "/";

        if (referer != null && referer.contains("/admin")) {
            targetUrl = "/login?logout";
            log.info(
                    "관리자 로그아웃 - 관리자 로그인 페이지로 리다이렉트: {}",
                    targetUrl
            );
        } else {
            targetUrl = "/login?logout";
            log.info(
                    "사용자 로그아웃 - 사용자 로그인 페이지로 리다이렉트: {}",
                    targetUrl
            );
        }

        response.sendRedirect(targetUrl);
    }
}