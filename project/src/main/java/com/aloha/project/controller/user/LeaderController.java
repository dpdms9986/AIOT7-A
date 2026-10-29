package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.TaskAssignRequest;
import com.aloha.project.dto.TeamMember;
import com.aloha.project.repository.lands.LandRepository;
import com.aloha.project.repository.tasks.DroneRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.tasks.TaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Controller 
@RequestMapping("/leader")
@RequiredArgsConstructor
public class LeaderController {

    private final TaskService taskService;
    private final LandRepository landRepository;
    private final UserRepository userRepository;
    private final DroneRepository droneRepository;

    // 1. 관제 대시보드
    /**
     * 1. 관제 대시보드 메인 (종합 관제 데이터 집계)
     */
    @GetMapping("/dashboard")
    public String getDashboard(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        // 👇 [핵심 해결] 현재 활성화된 세션에서 단장 엔티티를 새로 조회!
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;

        String teamName = (myTeam != null && myTeam.getTeamName() != null) ? myTeam.getTeamName() : "소속 방제단 없음";
        String inviteCode = (myTeam != null && myTeam.getInviteCode() != null) ? myTeam.getInviteCode() : "------";

        // 1. 우리 방제단 소속 조종사 수 집계
        List<Users> pilots = new ArrayList<>();
        if (myTeam != null && myTeam.getInviteCode() != null) {
            final String currentInvite = myTeam.getInviteCode();
            pilots = userRepository.findAll().stream()
                    .filter(u -> u.getTeam() != null 
                              && currentInvite.equals(u.getTeam().getInviteCode())
                              && !"LEADER".equals(u.getRole()))
                    .toList();
        }
        int totalPilotCount = pilots.size();

        // 2. 우리 방제단 보유 드론 기체 현황 집계
        List<Drones> drones = new ArrayList<>();
        if (myTeam != null && myTeam.getInviteCode() != null) {
            final String currentInvite = myTeam.getInviteCode();
            drones = droneRepository.findAll().stream()
                    .filter(d -> d.getUser() != null 
                              && d.getUser().getTeam() != null 
                              && currentInvite.equals(d.getUser().getTeam().getInviteCode()))
                    .toList();
        }
        long totalDroneCount = drones.size();
        long availableDroneCount = drones.stream().filter(d -> "AVAILABLE".equals(d.getStatus())).count(); // 대기/가용 기체
        long inUseDroneCount = drones.stream().filter(d -> "IN_USE".equals(d.getStatus())).count();         // 비행/작업 중 기체

        // 3. 전체 농지 및 방제 진행률 집계
        List<Lands> lands = landRepository.findAll();
        long totalLandCount = lands.size();
        long readyLandCount = lands.stream().filter(l -> "READY".equals(l.getStatus())).count();             // 방제 대기
        long assignedLandCount = lands.stream().filter(l -> "ASSIGNED".equals(l.getStatus())).count();       // 배정 완료
        long inProgressLandCount = lands.stream().filter(l -> "IN_PROGRESS".equals(l.getStatus())).count(); // 방제 진행 중
        long completedLandCount = lands.stream().filter(l -> "COMPLETED".equals(l.getStatus())).count();     // 방제 완료

        // 전체 방제 달성률 (0 ~ 100%)
        int progressPercent = (totalLandCount > 0) ? (int) ((completedLandCount * 100) / totalLandCount) : 0;

        // 4. Model에 관제 대시보드 위젯 데이터 전달
        model.addAttribute("leaderName", leader.getName());
        model.addAttribute("teamName", teamName);
        model.addAttribute("inviteCode", inviteCode);

        // 조종사 & 드론 지표
        model.addAttribute("totalPilotCount", totalPilotCount);
        model.addAttribute("totalDroneCount", totalDroneCount);
        model.addAttribute("availableDroneCount", availableDroneCount);
        model.addAttribute("inUseDroneCount", inUseDroneCount);

        // 농지 & 방제 현황 지표
        model.addAttribute("totalLandCount", totalLandCount);
        model.addAttribute("readyLandCount", readyLandCount);
        model.addAttribute("assignedLandCount", assignedLandCount);
        model.addAttribute("inProgressLandCount", inProgressLandCount);
        model.addAttribute("completedLandCount", completedLandCount);
        model.addAttribute("progressPercent", progressPercent);

        return "page/leader/dashboard";
    }

    // 2. 작업 배정
    @GetMapping("/task-assignment")
    public String getTaskAssignment(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        Users leader = customUser.getUser();
        Teams myTeam = leader.getTeam();

        // 1. 배정 가능한 농지 (READY 상태인 농지만)
        List<Lands> readyLands = landRepository.findByStatus("READY");

        // 2. 우리 방제단 소속 조종사 목록
        List<TeamMember> pilots = (myTeam != null)
                ? userRepository.findAll().stream()
                        .filter(u -> u.getTeam() != null 
                                  && myTeam.getInviteCode().equals(u.getTeam().getInviteCode())
                                  && !"LEADER".equals(u.getRole()))
                        .map(TeamMember::from)
                        .toList()
                : List.of();

        model.addAttribute("readyLands", readyLands);
        model.addAttribute("pilots", pilots);

        return "page/leader/task-assignment";
    }

    /**
     * 작업 배정 폼 제출 (POST)
     */
    @PostMapping("/task-assignment")
    public String assignTaskSubmit(@ModelAttribute TaskAssignRequest request,
                                   @AuthenticationPrincipal CustomUser customUser,
                                   RedirectAttributes redirectAttributes) {

        if (customUser == null) return "redirect:/login";

        boolean isSuccess = taskService.assignTask(customUser.getUser(), request);

        if (!isSuccess) {
            redirectAttributes.addFlashAttribute("errorMessage", "작업 배정에 실패했습니다.");
            return "redirect:/leader/task-assignment";
        }

        redirectAttributes.addFlashAttribute("successMessage", "작업 배정이 성공적으로 완료되었습니다!");
        return "redirect:/leader/task-assignment"; // 또는 /leader/farmlist
    }

    /**
     * 드론 기체 자산 관리 (실제 DB 연동)
     */
    @GetMapping("/assetlist")
    public String getAssetList(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        Users leader = customUser.getUser();
        Teams myTeam = leader.getTeam();

        List<Drones> drones = new ArrayList<>();

        // 1. 우리 방제단(Teams)에 소속된 유저들의 드론만 추출
        if (myTeam != null && myTeam.getInviteCode() != null) {
            final String inviteCode = myTeam.getInviteCode();
            drones = droneRepository.findAll().stream()
                    .filter(d -> d.getUser() != null 
                              && d.getUser().getTeam() != null 
                              && inviteCode.equals(d.getUser().getTeam().getInviteCode()))
                    .toList();
        }

        // 2. 상단 요약 카드 집계
        long totalCount = drones.size();
        long availableCount = drones.stream().filter(d -> "AVAILABLE".equals(d.getStatus())).count(); // 가용
        long inUseCount = drones.stream().filter(d -> "IN_USE".equals(d.getStatus())).count();         // 비행 중
        long maintenanceCount = drones.stream().filter(d -> "MAINTENANCE".equals(d.getStatus())).count(); // 점검 중

        // 3. Model 바인딩 (assetlist.html 에서 th:each="drone : ${drones}" 로 출력)
        model.addAttribute("drones", drones);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("inUseCount", inUseCount);
        model.addAttribute("maintenanceCount", maintenanceCount);

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

    @GetMapping("/farmlist")
    public String getFarmList(Model model) {
        // 1. 전체 농지 목록 조회
        List<Lands> lands = landRepository.findAll();

        // 2. 상단 요약 카드 집계
        long readyCount = lands.stream().filter(l -> "READY".equals(l.getStatus())).count();
        long assignedCount = lands.stream().filter(l -> "ASSIGNED".equals(l.getStatus())).count();
        long progressCount = lands.stream().filter(l -> "IN_PROGRESS".equals(l.getStatus())).count();
        long completedCount = lands.stream().filter(l -> "COMPLETED".equals(l.getStatus())).count();

        // 3. 모델에 전달 (farmlist.html 에서 th:each="land : ${lands}" 로 출력)
        model.addAttribute("lands", lands);
        model.addAttribute("totalCount", lands.size());
        model.addAttribute("readyCount", readyCount);
        model.addAttribute("assignedCount", assignedCount);
        model.addAttribute("progressCount", progressCount);
        model.addAttribute("completedCount", completedCount);

        return "page/leader/farmlist";
    }
}