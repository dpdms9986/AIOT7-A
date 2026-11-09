package com.aloha.project.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.users.TeamRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 모든 화면(헤더)에서 공통으로 쓰는 값을 Model 에 자동으로 넣어줌
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final TeamRepository teamRepository;

    @ModelAttribute
    public void addCommonAttributes(HttpServletRequest request, Model model) {

        model.addAttribute("currentUri", request.getRequestURI());

        // 시큐리티에 저장된 로그인 정보 꺼내기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = (auth == null) ? null : auth.getPrincipal();

        log.info("[헤더] uri={}, principal={}", request.getRequestURI(),
                 principal == null ? "null" : principal.getClass().getSimpleName());

        // 로그인 안 했으면 principal 이 문자열 "anonymousUser" → 여기서 끝
        if (!(principal instanceof CustomUser loginUser)) return;

        Users user = loginUser.getUser();
        model.addAttribute("loginName", user.getName());

        String role = user.getRole();
        String roleLabel = switch (role == null ? "" : role) {
            case "LEADER" -> "방제단장";
            case "PILOT"  -> "조종사";
            case "ADMIN"  -> "관리자";
            default       -> "-";
        };
        model.addAttribute("roleLabel", roleLabel);

        if (user.getTeam() != null) {
            teamRepository.findById(user.getTeam().getNo()).ifPresent(team -> {
                model.addAttribute("headerTeamName", team.getTeamName());
                if ("LEADER".equals(role)) {
                    model.addAttribute("headerInviteCode", team.getInviteCode());
                }
            });
        }

        log.info("[헤더] name={}, role={}, teamId={}", user.getName(), role,
                 user.getTeam() == null ? "없음" : user.getTeam().getNo());
    }
}