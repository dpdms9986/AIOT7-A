package com.aloha.project.controller.user;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.FarmPointDto;
import com.aloha.project.dto.LandDto;
import com.aloha.project.dto.TeamMemberDto;
import com.aloha.project.repository.lands.LandRepository;
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

    @Transactional(readOnly = true)
    @GetMapping
    public String getDashboard(
            @RequestParam(value = "landNo", required = false) Long landNo,
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        if (customUser == null) return "redirect:/login";

        // 로그인 단장 및 소속 방제단 조회
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;

        if (myTeam != null) {
        model.addAttribute("inviteCode", myTeam.getInviteCode());
        model.addAttribute("myTeamChannel", myTeam.getInviteCode()); 
        model.addAttribute("myTeamName", myTeam.getTeamName());       // (화면 표시용 방제단 이름 필요 시)
}
		List<Lands> rawLands = (myTeam != null) ? landRepository.findByTeam(myTeam) : List.of();

        // 활성 배정 작업 조회 및 맵핑
        List<WorkAssignments> activeAssignments = workAssignmentRepository.findAll().stream()
            .filter(w -> w.getLand() != null)
            .filter(w -> "ASSIGNED".equals(w.getStatus()) || "IN_PROGRESS".equals(w.getStatus()))
            .toList();

        Map<Long, WorkAssignments> assignmentMap = activeAssignments.stream()
            .collect(Collectors.toMap(w -> w.getLand().getNo(), w -> w, (a, b) -> b, LinkedHashMap::new));;

				// pilotName, assignedAt 채운 lands 목록 생성
				List<LandDto> lands = rawLands.stream().map(land -> {
						LandDto dto = LandDto.from(land);
						WorkAssignments wa = assignmentMap.get(land.getNo());
						if (wa != null && wa.getPilot() != null) {
								dto.setPilotName(wa.getPilot().getName());
								dto.setAssignedAt(wa.getCreatedAt());
						}
						return dto;
				}).toList();

        List<TeamMemberDto> pilots = (myTeam != null)
        ? userRepository.findByTeam(myTeam).stream()
                .filter(u -> "PILOT".equals(u.getRole()))
                .map(TeamMemberDto::from)
                .toList()
        : List.of();

				// 3. 화면 헤더에 초대코드 표기가 필요하다면 모델에 전달
				if (myTeam != null) {
				    model.addAttribute("inviteCode", myTeam.getInviteCode());
				
                    // 방제단 자동 관제 연결용
                    model.addAttribute("myTeamChannel", myTeam.getInviteCode());
                    model.addAttribute("myTeamName", myTeam.getTeamName());

                    }
        // ========================================================
        // 👇 작성한 자동 계산 및 Model 바인딩 (100% 보존!)
        // ========================================================

        // 1) 요약 카드 숫자
        model.addAttribute("readyCount",     countStatus(lands, "READY"));
        model.addAttribute("assignedCount",  countStatus(lands, "ASSIGNED"));
        model.addAttribute("progressCount",  countStatus(lands, "IN_PROGRESS"));
        model.addAttribute("completedCount", countStatus(lands, "COMPLETED"));
        
        // 2) 오른쪽 작업 중인 농지 목록
        List<LandDto> inProgressLands = lands.stream() .filter(l -> "IN_PROGRESS".equals(l.getStatus())).toList();
        model.addAttribute("inProgressLands", inProgressLands);

        // 3) 오른쪽 미배정 농지 목록
        List<LandDto> readyLands = lands.stream().filter(l -> "READY".equals(l.getStatus())).toList();
        model.addAttribute("readyLands", readyLands);

        // 4) 조종사별: 맡은 작업 수(배정+진행 중), 지금 작업 중인 농지 주소
        Map<String, Long> activeCount = lands.stream()
                .filter(l -> ("ASSIGNED".equals(l.getStatus()) || "IN_PROGRESS".equals(l.getStatus())) && l.getPilotName() != null)
                .collect(Collectors.groupingBy(LandDto::getPilotName, LinkedHashMap::new, Collectors.counting()));
        Map<String, String> workingAt = lands.stream()
                .filter(l -> "IN_PROGRESS".equals(l.getStatus()) && l.getPilotName() != null)
                .collect(Collectors.toMap(LandDto::getPilotName, LandDto::getAddress, (a, b) -> a, LinkedHashMap::new));

        model.addAttribute("pilots", pilots);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("workingAt", workingAt);

        // 5) 지도에 찍을 점 전부 (JS 변수로 넘김)
        model.addAttribute("farmPoints", lands.stream().map(FarmPointDto::from).toList());

        // 6) 처음 선택할 농지, 오늘 날짜
        model.addAttribute("selectedLandNo", landNo);
        model.addAttribute("today", LocalDate.now());

        return "page/leader/dashboard";
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