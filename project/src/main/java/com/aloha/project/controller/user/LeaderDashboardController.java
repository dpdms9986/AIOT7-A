package com.aloha.project.controller.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.tasks.DispatchBatches;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.FarmPointDto;
import com.aloha.project.dto.LandDto;
import com.aloha.project.dto.TeamMemberDto;
import com.aloha.project.repository.lands.LandRepository;
import com.aloha.project.repository.tasks.DispatchBatchRepository;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/leader/dashboard")
@RequiredArgsConstructor
public class LeaderDashboardController {

    private final UserRepository userRepository;
    private final LandRepository landRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final DispatchBatchRepository dispatchBatchRepository;

    @GetMapping
    public String getDashboard(
            @RequestParam(value = "landNo", required = false) Long landNo,
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        if (customUser == null) return "redirect:/login";

        // [DB 연동 1] 로그인 단장 및 소속 방제단 조회
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;
        final String inviteCode = (myTeam != null && myTeam.getInviteCode() != null) ? myTeam.getInviteCode() : "";

        // [DB 연동 2] 실제 농지 목록 조회 (LandDto 변환)
        List<LandDto> lands = landRepository.findAll().stream()
                .map(LandDto::from)
                .toList();

        // [DB 연동 3] 실제 우리 방제단 소속 조종사 목록 조회 (TeamMemberDto 변환)
        List<TeamMemberDto> pilots = new ArrayList<>();
        if (!inviteCode.isEmpty()) {
            pilots = userRepository.findAll().stream()
                    .filter(u -> u.getTeam() != null 
                              && inviteCode.equals(u.getTeam().getInviteCode())
                              && !"LEADER".equals(u.getRole()))
                    .map(TeamMemberDto::from)
                    .toList();
        }

        // ========================================================
        // 👇 프론트엔드 팀원이 작성한 자동 계산 및 Model 바인딩 (100% 보존!)
        // ========================================================

        // 1) 요약 카드 숫자
        model.addAttribute("readyCount",     countStatus(lands, "READY"));
        model.addAttribute("assignedCount",  countStatus(lands, "ASSIGNED"));
        model.addAttribute("progressCount",  countStatus(lands, "IN_PROGRESS"));
        model.addAttribute("completedCount", countStatus(lands, "COMPLETED"));

        // 2) 오른쪽 미배정 농지 목록
        List<LandDto> readyLands = lands.stream().filter(l -> "READY".equals(l.getStatus())).toList();
        model.addAttribute("readyLands", readyLands);

        // 3) 조종사별: 맡은 작업 수(배정+진행 중), 지금 작업 중인 농지 주소
        Map<String, Long> activeCount = lands.stream()
                .filter(l -> ("ASSIGNED".equals(l.getStatus()) || "IN_PROGRESS".equals(l.getStatus())) && l.getPilotName() != null)
                .collect(Collectors.groupingBy(LandDto::getPilotName, LinkedHashMap::new, Collectors.counting()));
        Map<String, String> workingAt = lands.stream()
                .filter(l -> "IN_PROGRESS".equals(l.getStatus()) && l.getPilotName() != null)
                .collect(Collectors.toMap(LandDto::getPilotName, LandDto::getAddress, (a, b) -> a, LinkedHashMap::new));

        model.addAttribute("pilots", pilots);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("workingAt", workingAt);

        // 4) 지도에 찍을 점 전부 (JS 변수로 넘김)
        model.addAttribute("farmPoints", lands.stream().map(FarmPointDto::from).toList());

        // 5) 처음 선택할 농지, 오늘 날짜
        model.addAttribute("selectedLandNo", landNo);
        model.addAttribute("today", LocalDate.now());

        return "page/leader/dashboard";
    }

    /**
     * 대시보드에서 바로 배정 (실제 DB 저장)
     */
    @PostMapping("/assign")
    public String assign(
            @RequestParam("landNo") Long landNo,
            @RequestParam("pilotNo") Long pilotNo,
            @RequestParam("workDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes redirectAttributes) {

        log.info("대시보드 배정 요청: landNo={}, pilotNo={}, workDate={}", landNo, pilotNo, workDate);

        if (customUser == null) return "redirect:/login";

        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Lands land = landRepository.findById(landNo).orElse(null);
        Users pilot = userRepository.findById(pilotNo).orElse(null);

        if (leader != null && land != null && pilot != null && leader.getTeam() != null) {
            // 상위 배정 묶음(DispatchBatches) 생성
            DispatchBatches batch = DispatchBatches.builder()
                    .team(leader.getTeam())
                    .leader(leader)
                    .batchTitle(workDate + " 관제 배정")
                    .totalArea(land.getAreaSize())
                    .assignedPilotCount(1)
                    .build();
            dispatchBatchRepository.save(batch);

            // 개별 작업 배정(WorkAssignments) 생성
            WorkAssignments assignment = WorkAssignments.builder()
                    .dispatchBatch(batch)
                    .land(land)
                    .pilot(pilot)
                    .status("ASSIGNED")
                    .build();
            workAssignmentRepository.save(assignment);

            // 농지 상태 변경
            land.updateStatus("ASSIGNED");
            landRepository.save(land);

            redirectAttributes.addFlashAttribute("message", landNo + "번 농지가 " + pilot.getName() + " 조종사에게 배정되었습니다.");
        }

        redirectAttributes.addAttribute("landNo", landNo);
        return "redirect:/leader/dashboard";
    }

    /**
     * 대시보드에서 배정 취소 (실제 DB 취소)
     */
    @PostMapping("/unassign")
    public String unassign(
            @RequestParam("landNo") Long landNo,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes redirectAttributes) {

        log.info("대시보드 배정 취소 요청: landNo={}", landNo);

        if (customUser == null) return "redirect:/login";

        Lands land = landRepository.findById(landNo).orElse(null);
        if (land != null) {
            // 기존 배정 내역 삭제
            List<WorkAssignments> assignments = workAssignmentRepository.findAll().stream()
                    .filter(w -> w.getLand() != null && landNo.equals(w.getLand().getNo()))
                    .toList();
            workAssignmentRepository.deleteAll(assignments);

            // 농지 상태 READY로 복구
            land.updateStatus("READY");
            landRepository.save(land);

            redirectAttributes.addFlashAttribute("message", landNo + "번 농지 배정이 취소되었습니다.");
        }

        redirectAttributes.addAttribute("landNo", landNo);
        return "redirect:/leader/dashboard";
    }

    private long countStatus(List<LandDto> lands, String status) {
        return lands.stream().filter(l -> status.equals(l.getStatus())).count();
    }
}