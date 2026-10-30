package com.aloha.project.controller.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.dto.FarmPointDto;
import com.aloha.project.dto.LandDto;
import com.aloha.project.dto.TeamMemberDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 방제단장 - 관제 대시보드
 * 예) /leader/dashboard?landNo=1  → 1번 농지를 선택한 채로 열기
 */
@Slf4j
@Controller
@RequestMapping("/leader")
public class LeaderDashboardController {

    @GetMapping("/dashboard")
    public String getDashboard(@RequestParam(value = "landNo", required = false) Long landNo,
                               Model model) {

        // TODO(백엔드): landService.getLands(로그인 단장의 팀), teamService.getMembers(...)
        List<LandDto> lands = sampleLands();
        List<TeamMemberDto> pilots = sampleMembers();

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
                .filter(l -> "ASSIGNED".equals(l.getStatus()) || "IN_PROGRESS".equals(l.getStatus()))
                .collect(Collectors.groupingBy(LandDto::getPilotName, LinkedHashMap::new, Collectors.counting()));
        Map<String, String> workingAt = lands.stream()
                .filter(l -> "IN_PROGRESS".equals(l.getStatus()))
                .collect(Collectors.toMap(LandDto::getPilotName, LandDto::getAddress, (a, b) -> a, LinkedHashMap::new));

        model.addAttribute("pilots", pilots);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("workingAt", workingAt);

        // 4) 지도에 찍을 점 전부 (JS 변수로 넘김)
        model.addAttribute("farmPoints", lands.stream().map(FarmPointDto::from).toList());

        // 5) 처음 선택할 농지, 오늘 날짜 (예정일 최소값)
        model.addAttribute("selectedLandNo", landNo);
        model.addAttribute("today", LocalDate.now());

        return "page/leader/dashboard";
    }

    /**
     * 대시보드에서 바로 배정 (농지 1개 → 조종사 1명)
     * POST /leader/dashboard/assign  (landNo, pilotNo, workDate)
     */
    @PostMapping("/dashboard/assign")
    public String assign(@RequestParam("landNo") Long landNo,
                         @RequestParam("pilotNo") Long pilotNo,
                         @RequestParam("workDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
                         RedirectAttributes redirectAttributes) {
        log.info("대시보드 배정 요청: landNo={}, pilotNo={}, workDate={}", landNo, pilotNo, workDate);
        // TODO(백엔드): workAssignmentService.assignOne(로그인 단장의 팀, landNo, pilotNo, workDate)
        redirectAttributes.addAttribute("landNo", landNo);
        redirectAttributes.addFlashAttribute("message", landNo + "번 농지 배정 요청을 받았습니다. (샘플)");
        return "redirect:/leader/dashboard";
    }

    /**
     * 대시보드에서 배정 취소 (ASSIGNED 만)
     * POST /leader/dashboard/unassign  (landNo)
     */
    @PostMapping("/dashboard/unassign")
    public String unassign(@RequestParam("landNo") Long landNo,
                           RedirectAttributes redirectAttributes) {
        log.info("대시보드 배정 취소 요청: landNo={}", landNo);
        // TODO(백엔드): workAssignmentService.cancel(로그인 단장의 팀, landNo)
        redirectAttributes.addAttribute("landNo", landNo);
        redirectAttributes.addFlashAttribute("message", landNo + "번 농지 배정 취소 요청을 받았습니다. (샘플)");
        return "redirect:/leader/dashboard";
    }

    private long countStatus(List<LandDto> lands, String status) {
        return lands.stream().filter(l -> status.equals(l.getStatus())).count();
    }

    /* =========================================================
       샘플 데이터 (LeaderController 와 같은 내용, 백엔드 연결 전까지만)
       ========================================================= */
    private List<TeamMemberDto> sampleMembers() {
        List<TeamMemberDto> list = new ArrayList<>();
        list.add(TeamMemberDto.builder().no(1L).name("김민수").status("working").build());
        list.add(TeamMemberDto.builder().no(2L).name("이서연").status("online").build());
        list.add(TeamMemberDto.builder().no(3L).name("박지훈").status("online").build());
        list.add(TeamMemberDto.builder().no(4L).name("최유진").status("offline").build());
        return list;
    }

    private List<LandDto> sampleLands() {
        List<LandDto> list = new ArrayList<>();
        list.add(LandDto.builder().no(1L).city("대전광역시").region("유성구").dong("전민동")
                .address("전민동 15-2").latitude(36.3912).longitude(127.4035).areaSize(3300.0)
                .status("READY").build());
        list.add(LandDto.builder().no(2L).city("대전광역시").region("유성구").dong("노은동")
                .address("노은동 2-8").latitude(36.3741).longitude(127.3189).areaSize(2150.0)
                .status("ASSIGNED").pilotName("이서연")
                .assignedAt(LocalDateTime.of(2026, 10, 29, 9, 30)).build());
        list.add(LandDto.builder().no(3L).city("대전광역시").region("유성구").dong("전민동")
                .address("전민동 31-7").latitude(36.3890).longitude(127.4071).areaSize(4800.0)
                .status("IN_PROGRESS").pilotName("김민수")
                .assignedAt(LocalDateTime.of(2026, 10, 29, 8, 0)).build());
        list.add(LandDto.builder().no(4L).city("대전광역시").region("유성구").dong("노은동")
                .address("노은동 112").latitude(null).longitude(null).areaSize(1200.0)
                .status("COMPLETED").pilotName("박지훈")
                .assignedAt(LocalDateTime.of(2026, 10, 28, 14, 0)).build());
        list.add(LandDto.builder().no(5L).city("대전광역시").region("유성구").dong("전민동")
                .address("전민동 42-1").latitude(36.3931).longitude(127.4012).areaSize(2600.0)
                .status("READY").build());
        list.add(LandDto.builder().no(6L).city("대전광역시").region("유성구").dong("노은동")
                .address("노은동 77-3").latitude(36.3769).longitude(127.3151).areaSize(1850.0)
                .status("READY").build());
        list.add(LandDto.builder().no(7L).city("대전광역시").region("유성구").dong("구즉동")
                .address("구즉동 9-4").latitude(36.4210).longitude(127.3902).areaSize(5100.0)
                .status("READY").build());
        return list;
    }
}