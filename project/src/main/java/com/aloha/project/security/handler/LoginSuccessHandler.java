package com.aloha.project.security.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final WorkAssignmentRepository workAssignmentRepository;

    private final RequestCache requestCache = new HttpSessionRequestCache();

    public LoginSuccessHandler(WorkAssignmentRepository workAssignmentRepository) {
        this.workAssignmentRepository = workAssignmentRepository;
    }

    /**
     * 이전 조종사의 작업이 STOPPED 상태이고,
     * 같은 배정 묶음 및 농지에 다른 조종사의 작업이 존재하면 재배정된 작업으로 판단합니다.
     */
    private boolean isReassigned(WorkAssignments assignment) {

        if (assignment == null
                || !"STOPPED".equals(assignment.getStatus())
                || assignment.getLand() == null
                || assignment.getDispatchBatch() == null
                || assignment.getPilot() == null) {
            return false;
        }

        Long previousPilotNo = assignment.getPilot().getNo();
        Long batchNo = assignment.getDispatchBatch().getNo();

        return workAssignmentRepository.findByLand(assignment.getLand())
                .stream()
                .anyMatch(other -> other != null
                        && !other.getNo().equals(assignment.getNo())
                        && other.getDispatchBatch() != null
                        && batchNo.equals(other.getDispatchBatch().getNo())
                        && other.getPilot() != null
                        && !previousPilotNo.equals(other.getPilot().getNo()));
    }

    /**
     * 로그인한 조종사에게 강제 중단 작업 또는 미확인 재배정 알림이 있는지 확인
     * 알림이 존재하면 일반 로그인 메시지를 생략하여 프론트엔드의 중단 작업 확인 흐름과 일반 로그인 팝업이 동시에 노출되지 않도록함
     */
    @Override
    @Transactional(readOnly = true)
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        log.info("로그인 성공: {}", authentication.getName());

        if (!(authentication.getPrincipal() instanceof CustomUser customUser)
                || customUser.getUser() == null) {
            log.warn("로그인 principal이 CustomUser 형식이 아닙니다.");
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        HttpSession session = request.getSession();
        session.setAttribute("user", customUser.getUser());

        FlashMap flashMap = new FlashMap();
        boolean hasInterruptedWork = false;

        // 조종사 로그인 시 강제 중단 작업 또는 미확인 재배정 알림 존재 여부 확인
        if ("PILOT".equals(customUser.getUser().getRole())) {

            hasInterruptedWork = workAssignmentRepository
                    .findByPilot_No(customUser.getUser().getNo())
                    .stream()
                    .anyMatch(assignment -> {
                        // 재배정 알림은 확인 처리 전까지 로그인 시 안내 대상
                        if (isReassigned(assignment)) {
                            return assignment.getReassignmentNoticeReadAt() == null;
                        }

                        // 현재 본인에게 남아 있는 강제 중단 작업
                        return "INTERRUPTED".equals(assignment.getStatus());
                    });

            if (hasInterruptedWork) {
                flashMap.put("hasInterruptedWork", true);
                log.info("[중단 작업 확인 필요] 조종사 번호={}",
                        customUser.getUser().getNo());
            }
        }

        // 중단/재배정 안내가 있으면 일반 로그인 메시지는 생략
        if (!hasInterruptedWork) {
            flashMap.put(
                    "loginMessage",
                    customUser.getUser().getName() + "님, 로그인되었습니다."
            );
        }

        new SessionFlashMapManager().saveOutputFlashMap(
                flashMap, request, response
        );

        // 아이디 저장
        String rememberId = request.getParameter("remember-id");
        String username = request.getParameter("username");

        // 아이디 저장 체크
        if ("on".equals(rememberId) && username != null && !username.isBlank()) {
            Cookie cookie = new Cookie("remember-id", username);
            cookie.setMaxAge(60 * 60 * 24 * 7);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        // 아이디 저장 체크 해제
        else {
            Cookie cookie = new Cookie("remember-id", "");
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        // redirect 파라미터가 있으면 해당 페이지로 이동
        String redirect = request.getParameter("redirect");

        if (redirect != null && !redirect.isBlank()) {
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

        // 기본 페이지로 이동
        log.info("기본 페이지로 이동");
        response.sendRedirect("/");
    }
}
