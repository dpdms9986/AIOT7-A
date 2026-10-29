package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.TeamMember;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Controller 
@RequestMapping("/leader")
@RequiredArgsConstructor
public class LeaderController {

    private final UserRepository userRepository;

    // 1. 관제 대시보드
    @GetMapping("/dashboard")
    public String getDashboard() {
        return "page/leader/dashboard";
    }

    // 2. 작업 배정
    @GetMapping("/task-assignment")
    public String getTaskAssignment() {
        return "page/leader/task-assignment";
    }

    // 3. 농지 관리
    @GetMapping("/farmlist")
    public String getFarmList() {
        return "page/leader/farmlist";
    }

    // 4. 드론 자산
    @GetMapping("/assetlist")
    public String getAssetList() {
        return "page/leader/assetlist";
    }

    // 5. 작업 이력
    @GetMapping("/history")
    public String getHistory() {
        return "page/pilot/history";
    }

    // 6. 방제단 관리 (실제 DB 데이터 조회 & 검색)
    @GetMapping("/team-management")
    public String getTeamManagement(
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "status",  defaultValue = "") String status,
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        // 로그인하지 않고 주소창으로 직접 들어온 경우 방어
        if (customUser == null) {
            log.warn("로그인 정보 없음 -> 로그인 페이지로 리다이렉트");
            return "redirect:/login";
        }

        Users loginLeader = customUser.getUser();
        String teamName = "소속 방제단 없음";
        String inviteCode = "------";

        List<TeamMember> allMembers = new ArrayList<>();

        // 단장이 소속된 방제단 정보와 조종사 목록 조회
        if (loginLeader != null && loginLeader.getTeam() != null) {
            var myTeam = loginLeader.getTeam();
            if (myTeam.getTeamName() != null) teamName = myTeam.getTeamName();
            if (myTeam.getInviteCode() != null) inviteCode = myTeam.getInviteCode();

            try {
                final String currentInviteCode = inviteCode;
                // DB에서 같은 방제단 초대코드를 가진 조종사들만 추출
                allMembers = userRepository.findAll().stream()
                        .filter(u -> u.getTeam() != null 
                                  && currentInviteCode.equals(u.getTeam().getInviteCode())
                                  && !"LEADER".equals(u.getRole()))
                        .map(TeamMember::from)
                        .toList();
            } catch (Exception e) {
                log.error("조종사 목록 조회 중 에러: {}", e.getMessage());
            }
        }

        // 검색어(이름/전화번호/아이디) 및 접속 상태 필터링
        String kw = (keyword != null) ? keyword.trim() : "";
        List<TeamMember> members = allMembers.stream()
                .filter(m -> kw.isEmpty()
                          || (m.getName() != null && m.getName().contains(kw))
                          || (m.getPhone() != null && m.getPhone().contains(kw))
                          || (m.getUsername() != null && m.getUsername().contains(kw)))
                .filter(m -> status == null || status.isEmpty() || status.equals(m.getStatus()))
                .toList();

        // 요약 카드 통계 계산
        long onlineCount  = allMembers.stream().filter(m -> "online".equals(m.getStatus())).count();
        long workingCount = allMembers.stream().filter(m -> "working".equals(m.getStatus())).count();
        int completedSum  = allMembers.stream().mapToInt(TeamMember::getCompleted).sum();

        // 접속 중인 조종사 목록
        List<TeamMember> onlineMembers = allMembers.stream()
                .filter(m -> "online".equals(m.getStatus()))
                .toList();

        // Model에 템플릿 바인딩 데이터 전달
        model.addAttribute("teamName", teamName);
        model.addAttribute("inviteCode", inviteCode);
        model.addAttribute("members", members);
        model.addAttribute("onlineMembers", onlineMembers);
        model.addAttribute("totalCount", allMembers.size());
        model.addAttribute("onlineCount", onlineCount);
        model.addAttribute("workingCount", workingCount);
        model.addAttribute("completedSum", completedSum);

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "page/leader/team-management";
    }

    // 7. 작업 재배정 (폼 전송)
    @PostMapping("/team-management/{no}/reassign")
    public String reassign(@PathVariable("no") Long no,
                           @RequestParam("targetNo") Long targetNo,
                           RedirectAttributes redirectAttributes) {
        log.info("작업 재배정 요청: {} → {}", no, targetNo);
        redirectAttributes.addFlashAttribute("message", "재배정 요청이 처리되었습니다.");
        return "redirect:/leader/team-management";
    }
}