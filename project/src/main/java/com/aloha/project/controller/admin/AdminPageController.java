package com.aloha.project.controller.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.AdminWorkDto; // dto/admin 폴더에 두었다면: com.aloha.project.dto.admin.AdminWorkDto
import com.aloha.project.dto.LandDto;
import com.aloha.project.repository.lands.LandRepository;
import com.aloha.project.repository.tasks.DroneRepository;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.TeamRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시스템 관리자(ADMIN) 전용 화면 컨트롤러
 * - ROLE_ADMIN 권한만 접근 가능
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // 👈 관리자 권한 필수 검증
public class AdminPageController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LandRepository landRepository;
    private final DroneRepository droneRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    /**
     * 1. 관리자 통합 관제 대시보드
     * GET /admin 또는 GET /admin/dashboard
     */
    @Transactional(readOnly = true) // open-in-view=false 라서 LAZY(농지/조종사)는 이 안에서 꺼내야 함
    @GetMapping({"", "/", "/dashboard"})
    public String getDashboard(@AuthenticationPrincipal CustomUser customUser, Model model) {
        log.info("관리자 대시보드 접근: {}", customUser != null ? customUser.getUsername() : "anonymous");

        // 1. 시스템 전체 회원 현황
        List<Users> allUsers = userRepository.findAll();
        long leaderCount = allUsers.stream().filter(u -> "LEADER".equals(u.getRole())).count();
        long pilotCount  = allUsers.stream().filter(u -> "PILOT".equals(u.getRole())).count();

        // 2. 등록된 전체 방제단(Teams) 현황
        long totalTeamCount = teamRepository.findAll().size();

        // 3. 등록된 전체 드론 기체 현황
        List<Drones> allDrones = droneRepository.findAll();
        long totalDroneCount = allDrones.size();
        long availableDroneCount = allDrones.stream().filter(d -> "AVAILABLE".equals(d.getStatus())).count();

        // 4. 시스템 전체 농지 및 방제 실적 집계
        List<Lands> allLands = landRepository.findAll();
        long totalLandCount = allLands.size();
        long completedLandCount = allLands.stream().filter(l -> "COMPLETED".equals(l.getStatus())).count();

        // 전체 방제 완료율 계산
        int totalProgressPercent = (totalLandCount > 0) ? (int) ((completedLandCount * 100) / totalLandCount) : 0;

        // 5. 최근 방제 작업 이력 (8건)
        // TODO(백엔드) findAll() 은 전체를 읽어서 느릴 수 있음.
        //   WorkAssignmentRepository 에 findTop8ByOrderByCreatedAtDesc() 만들어지면 아래 줄 교체:
        //   List<WorkAssignments> allWorks = workAssignmentRepository.findTop8ByOrderByCreatedAtDesc();
        List<WorkAssignments> allWorks = workAssignmentRepository.findAll();
        List<AdminWorkDto> recentWorks = allWorks.stream()
                .map(AdminWorkDto::from)
                .filter(w -> w.getWorkDate() != null)
                .sorted(Comparator.comparing(AdminWorkDto::getWorkDate).reversed())
                .limit(8)
                .toList();

        // 6. 주간(최근 7일) 방제 완료 추이 - 0건인 날도 채움
        // TODO(백엔드) findByStatusAndCompletedAtGreaterThanEqual("COMPLETED", 시작일시) 만들어지면
        //   allWorks 대신 그 결과를 사용
        LocalDate today = LocalDate.now();
        List<String> weekLabels = new ArrayList<>();
        List<Long> weekCounts = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            weekLabels.add(String.format("%02d.%02d", day.getMonthValue(), day.getDayOfMonth()));
            weekCounts.add(allWorks.stream()
                    .filter(w -> "COMPLETED".equals(w.getStatus())
                            && w.getCompletedAt() != null
                            && w.getCompletedAt().toLocalDate().equals(day))
                    .count());
        }

        // Model 바인딩
        model.addAttribute("totalTeamCount", totalTeamCount);
        model.addAttribute("leaderCount", leaderCount);
        model.addAttribute("pilotCount", pilotCount);
        model.addAttribute("totalDroneCount", totalDroneCount);
        model.addAttribute("availableDroneCount", availableDroneCount);
        model.addAttribute("totalLandCount", totalLandCount);
        model.addAttribute("completedLandCount", completedLandCount);
        model.addAttribute("totalProgressPercent", totalProgressPercent);
        model.addAttribute("recentWorks", recentWorks);
        model.addAttribute("weekLabels", weekLabels);
        model.addAttribute("weekCounts", weekCounts);

        


        return "page/admin/dashboard"; // templates/page/admin/dashboard.html
    }

    /**
     * 2. 전체 농지 통합 관리
     * GET /admin/lands
     */
    @GetMapping("/lands")
    public String getLands(
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            Model model) {

        List<Lands> allLands = landRepository.findAll();

        String kw = keyword.trim();
        List<LandDto> lands = allLands.stream()
                .filter(l -> status.isEmpty() || status.equals(l.getStatus()))
                .filter(l -> kw.isEmpty() || (l.getAddress() != null && l.getAddress().contains(kw)))
                .map(LandDto::from)
                .toList();

        model.addAttribute("lands", lands);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);

        return "page/admin/lands"; // templates/page/admin/lands.html
    }

    /**
     * 3. 전체 방제단 통합 관리
     * GET /admin/team-management
     */
    @GetMapping("/team-management")
    public String getTeamManagement(Model model) {
        // 시스템에 등록된 모든 방제단 목록 조회
        List<Teams> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);

        return "page/admin/team-management"; // templates/page/admin/team-management.html
    }

    /**
     * 4. 시스템 종합 방제 통계
     * GET /admin/statistics
     */
    // TODO(백엔드) w.getLand() 가 LAZY 라서 @Transactional(readOnly = true) 필요 (open-in-view=false)
    @GetMapping("/statistics")
    public String getStatistics(Model model) {
        // 전체 완료된 작업 이력 조회
        List<WorkAssignments> completedTasks = workAssignmentRepository.findAll().stream()
                .filter(w -> "COMPLETED".equals(w.getStatus()))
                .toList();

        // 총 방제 완료 면적(㎡) 합산
        double totalCompletedArea = completedTasks.stream()
                .mapToDouble(w -> (w.getLand() != null && w.getLand().getAreaSize() != null) ? w.getLand().getAreaSize() : 0.0)
                .sum();

        model.addAttribute("completedTaskCount", completedTasks.size());
        model.addAttribute("totalCompletedArea", totalCompletedArea);

        return "page/admin/statistics"; // templates/page/admin/statistics.html
    }
}