package com.aloha.project.controller.user;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.dto.AssignRequest;
import com.aloha.project.dto.LandCreateRequest;
import com.aloha.project.dto.LandDto;
import com.aloha.project.dto.PilotTaskDto;
import com.aloha.project.dto.TeamMemberDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 방제단장(LEADER) 화면 컨트롤러
 * - 주소가 /leader 로 시작하는 요청을 처리
 * - 지금은 샘플 데이터 사용 → TODO(백엔드) 부분을 서비스 호출로 교체 예정
 */
@Slf4j
@Controller
@RequestMapping("/leader")
public class LeaderController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    /* =========================================================
       작업 배정 (탭 3개: assign / status / history)
       예) /leader/task-assignment?tab=history&from=2026-10-01&to=2026-10-31&hPilot=김민수
       ========================================================= */
    @GetMapping("/task-assignment")
    public String getTaskAssignment(
            @RequestParam(value = "tab",     defaultValue = "assign") String tab,
            // [배정 탭]
            @RequestParam(value = "region",  defaultValue = "") String region,
            @RequestParam(value = "landNo",  required = false) Long landNo,
            // [현황 탭]
            @RequestParam(value = "status",  defaultValue = "") String status,
            @RequestParam(value = "pilot",   defaultValue = "") String pilot,
            // [이력 탭]
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "hRegion", defaultValue = "") String hRegion,
            @RequestParam(value = "hPilot",  defaultValue = "") String hPilot,
            Model model) {

        // TODO(백엔드): landService / teamService 로 교체
        List<LandDto> allLands = sampleLands();
        List<TeamMemberDto> pilots = sampleMembers();

        /* ----- [배정 탭] 미배정(READY) 농지 + 지역 검색 ----- */
        String[] words = region.trim().isEmpty() ? new String[0] : region.trim().split("\\s+");
        List<LandDto> readyLands = allLands.stream()
                .filter(l -> "READY".equals(l.getStatus()))
                .filter(l -> Arrays.stream(words)
                        .allMatch(w -> (l.getFullRegion() + " " + l.getAddress()).contains(w)))
                .toList();

        /* ----- [현황 탭] 배정됨 + 작업 중 ----- */
        List<LandDto> allAssigned = allLands.stream()
                .filter(l -> "ASSIGNED".equals(l.getStatus()) || "IN_PROGRESS".equals(l.getStatus()))
                .toList();
        List<LandDto> assigned = allAssigned.stream()
                .filter(l -> status.isEmpty() || l.getStatus().equals(status))
                .filter(l -> pilot.isEmpty()  || pilot.equals(l.getPilotName()))
                .toList();
        Map<String, Long> countByPilot = allAssigned.stream()
                .collect(Collectors.groupingBy(LandDto::getPilotName, LinkedHashMap::new, Collectors.counting()));

        /* ----- [이력 탭] 완료된 작업 + 기간/지역/조종사 검색 ----- */
        boolean dateError = from != null && to != null && from.isAfter(to);
        List<PilotTaskDto> history = dateError ? List.of() : filterHistory(from, to, hRegion, hPilot);
        double hArea = history.stream().mapToDouble(h -> h.getAreaSize() == null ? 0 : h.getAreaSize()).sum();
        double hUsed = history.stream().mapToDouble(h -> h.getUsedAmount() == null ? 0 : h.getUsedAmount()).sum();
        List<String> historyPilots = sampleTeamHistory().stream()
                .map(PilotTaskDto::getPilotName).distinct().sorted().toList();

        /* ----- 화면으로 보낼 값 ----- */
        model.addAttribute("tab", tab);
        // 배정 탭
        model.addAttribute("readyLands", readyLands);
        model.addAttribute("pilots", pilots);
        model.addAttribute("region", region);
        model.addAttribute("landNo", landNo);
        // 현황 탭
        model.addAttribute("assigned", assigned);
        model.addAttribute("assignedTotal", allAssigned.size());
        model.addAttribute("countByPilot", countByPilot);
        model.addAttribute("status", status);
        model.addAttribute("pilot", pilot);
        // 이력 탭
        model.addAttribute("history", history);
        model.addAttribute("hArea", hArea);
        model.addAttribute("hUsed", hUsed);
        model.addAttribute("historyPilots", historyPilots);
        model.addAttribute("dateError", dateError);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("hRegion", hRegion);
        model.addAttribute("hPilot", hPilot);

        return "page/leader/task-assignment";
    }

    /**
     * 작업 이력 CSV 다운로드 (화면과 같은 검색 조건)
     * GET /leader/task-assignment/history.csv?from=&to=&hRegion=&hPilot=
     */
    @GetMapping("/task-assignment/history.csv")
    public ResponseEntity<byte[]> downloadHistoryCsv(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "hRegion", defaultValue = "") String hRegion,
            @RequestParam(value = "hPilot",  defaultValue = "") String hPilot) {

        List<PilotTaskDto> rows = filterHistory(from, to, hRegion, hPilot);

        StringBuilder sb = new StringBuilder("\uFEFF");   // 엑셀 한글 깨짐 방지
        sb.append("완료 일시,주소,행정구역,면적(㎡),담당 조종사,배정 일시,사용 약제,사용량(L)\r\n");
        for (PilotTaskDto r : rows) {
            sb.append(String.join(",",
                    csv(r.getCompletedAt() == null ? "" : r.getCompletedAt().format(DT)),
                    csv(r.getAddress()),
                    csv(r.getRegion()),
                    csv(r.getAreaSize()),
                    csv(r.getPilotName()),
                    csv(r.getAssignedAt() == null ? "" : r.getAssignedAt().format(DT)),
                    csv(r.getChemicalName()),
                    csv(r.getUsedAmount())))
              .append("\r\n");
        }

        String fileName = URLEncoder.encode("작업이력_" + LocalDate.now() + ".csv", StandardCharsets.UTF_8)
                                    .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 작업 배정 - 배정 확정
     */
    @PostMapping("/task-assignment")
    public String assign(@ModelAttribute AssignRequest req,
                         RedirectAttributes redirectAttributes) {
        log.info("배정 요청: {}", req);

        if (req.getLandNos().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "배정할 농지를 선택하세요.");
            return "redirect:/leader/task-assignment";
        }
        if (req.totalQuota() != req.getLandNos().size()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "조종사별 배정 수 합계(" + req.totalQuota() + ")가 선택한 농지 수("
                    + req.getLandNos().size() + ")와 다릅니다.");
            return "redirect:/leader/task-assignment";
        }

        // TODO(백엔드): workAssignmentService.assign(로그인 단장의 팀, req)
        long pilotCount = req.getQuotas().values().stream().filter(v -> v != null && v > 0).count();
        redirectAttributes.addFlashAttribute("message",
                req.getLandNos().size() + "필지를 " + pilotCount + "명의 조종사에게 배정했습니다. (샘플)");
        return "redirect:/leader/task-assignment?tab=status";
    }

    /**
     * 작업 배정 - 배정 취소 (ASSIGNED 상태만)
     */
    @PostMapping("/task-assignment/cancel")
    public String cancelAssignment(@RequestParam("landNo") Long landNo,
                                   RedirectAttributes redirectAttributes) {
        log.info("배정 취소 요청: {}", landNo);
        // TODO(백엔드): workAssignmentService.cancel(로그인 단장의 팀, landNo)
        redirectAttributes.addFlashAttribute("message", landNo + "번 농지 배정 취소 요청을 받았습니다. (샘플)");
        return "redirect:/leader/task-assignment?tab=status";
    }

    /* =========================================================
       방제단 관리
       ========================================================= */
    @GetMapping("/team-management")
    public String getTeamManagement(
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "status",  defaultValue = "") String status,
            @RequestParam(value = "skill",   defaultValue = "") String skill,
            Model model) {

        List<TeamMemberDto> allMembers = sampleMembers();   // TODO(백엔드)

        String kw = keyword.trim();
        List<TeamMemberDto> members = allMembers.stream()
                .filter(m -> kw.isEmpty()
                          || m.getName().contains(kw)
                          || m.getPhone().contains(kw)
                          || m.getUsername().contains(kw))
                .filter(m -> status.isEmpty() || m.getStatus().equals(status))
                .filter(m -> skill.isEmpty()  || m.getSkill().equals(skill))
                .toList();

        long onlineCount  = allMembers.stream().filter(m -> "online".equals(m.getStatus())).count();
        long workingCount = allMembers.stream().filter(m -> "working".equals(m.getStatus())).count();
        int completedSum  = allMembers.stream().mapToInt(TeamMemberDto::getCompleted).sum();

        List<TeamMemberDto> onlineMembers = allMembers.stream()
                .filter(m -> "online".equals(m.getStatus()))
                .toList();

        model.addAttribute("teamName", "대전 유성 방제단");   // TODO(백엔드)
        model.addAttribute("inviteCode", "DR789A");           // TODO(백엔드)
        model.addAttribute("members", members);
        model.addAttribute("onlineMembers", onlineMembers);
        model.addAttribute("totalCount", allMembers.size());
        model.addAttribute("onlineCount", onlineCount);
        model.addAttribute("workingCount", workingCount);
        model.addAttribute("completedSum", completedSum);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("skill", skill);

        return "page/leader/team-management";
    }

    @PostMapping("/team-management/{no}/reassign")
    public String reassign(@PathVariable("no") Long no,
                           @RequestParam("targetNo") Long targetNo,
                           RedirectAttributes redirectAttributes) {
        log.info("재배정 요청: {} → {}", no, targetNo);
        // TODO(백엔드): workAssignmentService.reassign(no, targetNo)
        redirectAttributes.addFlashAttribute("message", "재배정 요청이 처리되었습니다. (샘플)");
        return "redirect:/leader/team-management";
    }

    /* =========================================================
       농지 관리
       ========================================================= */
    @GetMapping("/farmlist")
    public String getFarmList(
            @RequestParam(value = "dong",    defaultValue = "") String dong,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            Model model) {

        List<LandDto> allLands = sampleLands();   // TODO(백엔드)

        String kw = keyword.trim();
        List<LandDto> lands = allLands.stream()
                .filter(l -> dong.isEmpty() || l.getDong().equals(dong))
                .filter(l -> kw.isEmpty()
                          || (l.getFullRegion() + " " + l.getAddress()).contains(kw))
                .toList();

        List<String> dongs = allLands.stream()
                .map(LandDto::getDong)
                .distinct()
                .sorted()
                .toList();

        model.addAttribute("lands", lands);
        model.addAttribute("dongs", dongs);
        model.addAttribute("dong", dong);
        model.addAttribute("keyword", keyword);

        return "page/leader/farmlist";
    }

    @PostMapping("/farmlist")
    public String createLand(@ModelAttribute LandCreateRequest form,
                             RedirectAttributes redirectAttributes) {
        log.info("농지 등록 요청: {}", form);
        // TODO(백엔드): landService.create(로그인 단장의 팀, form)
        redirectAttributes.addFlashAttribute("message",
                form.getAddress() + " 농지 등록 요청을 받았습니다. (샘플이라 목록에는 추가되지 않아요)");
        return "redirect:/leader/farmlist";
    }

    @PostMapping("/farmlist/delete")
    public String deleteLand(@RequestParam("landNo") Long landNo,
                             RedirectAttributes redirectAttributes) {
        log.info("농지 삭제 요청: {}", landNo);
        // TODO(백엔드): landService.delete(로그인 단장의 팀, landNo)  ※ READY 만 허용
        redirectAttributes.addFlashAttribute("message", landNo + "번 농지 삭제 요청을 받았습니다. (샘플)");
        return "redirect:/leader/farmlist";
    }

    /* =========================================================
       작업 이력 → 작업 배정 화면의 "작업 이력" 탭으로 이동
       ========================================================= */
    @GetMapping("/history")
    public String getHistory() {
        return "redirect:/leader/task-assignment?tab=history";
    }

    

    /* =========================================================
       작업 이력 검색 (화면 + CSV 같이 사용)
       ========================================================= */
    private List<PilotTaskDto> filterHistory(LocalDate from, LocalDate to, String region, String pilot) {
        // TODO(백엔드): workAssignmentService.getTeamHistory(로그인 단장의 팀, from, to, region, pilot)
        String[] words = region.trim().isEmpty() ? new String[0] : region.trim().split("\\s+");
        return sampleTeamHistory().stream()
                .filter(h -> from == null || !h.getCompletedAt().toLocalDate().isBefore(from))
                .filter(h -> to == null   || !h.getCompletedAt().toLocalDate().isAfter(to))
                .filter(h -> Arrays.stream(words)
                        .allMatch(w -> (h.getRegion() + " " + h.getAddress()).contains(w)))
                .filter(h -> pilot.isEmpty() || pilot.equals(h.getPilotName()))
                .sorted(Comparator.comparing(PilotTaskDto::getCompletedAt).reversed())   // 최근 완료 먼저
                .toList();
    }

    // CSV 칸 하나: 따옴표로 감싸고, 안의 따옴표는 두 번 써서 깨지지 않게
    private String csv(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /* =========================================================
       샘플 데이터 (백엔드 연결 전까지만 사용)
       ========================================================= */
    private List<TeamMemberDto> sampleMembers() {
        List<TeamMemberDto> list = new ArrayList<>();
        list.add(TeamMemberDto.builder().no(1L).name("김민수").username("pilot1@test.com")
                .phone("010-1234-5678").status("working").drone("DJI T40-01").tank(40)
                .currentTask("유성구 3번 농지 방제").completed(3).skill("베테랑").build());
        list.add(TeamMemberDto.builder().no(2L).name("이서연").username("pilot2@test.com")
                .phone("010-2345-6789").status("online").drone("DJI T30-02").tank(30)
                .currentTask("-").completed(2).skill("중급").build());
        list.add(TeamMemberDto.builder().no(3L).name("박지훈").username("pilot3@test.com")
                .phone("010-3456-7890").status("online").drone(null).tank(null)
                .currentTask("-").completed(1).skill("초급").build());
        list.add(TeamMemberDto.builder().no(4L).name("최유진").username("pilot4@test.com")
                .phone("010-4567-8901").status("offline").drone("DJI T25-03").tank(20)
                .currentTask("-").completed(0).skill("중급").build());
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

    private List<PilotTaskDto> sampleTeamHistory() {
        List<PilotTaskDto> list = new ArrayList<>();
        list.add(PilotTaskDto.builder().no(4L).address("노은동 112").region("유성구 노은동").areaSize(1200.0)
                .status("COMPLETED").pilotName("박지훈").chemicalName("살균제 A").usedAmount(1.4)
                .assignedAt(LocalDateTime.of(2026, 10, 28, 14, 0))
                .completedAt(LocalDateTime.of(2026, 10, 28, 15, 10)).build());
        list.add(PilotTaskDto.builder().no(10L).address("전민동 8-11").region("유성구 전민동").areaSize(1500.0)
                .status("COMPLETED").pilotName("김민수").chemicalName("살충제 B").usedAmount(1.8)
                .assignedAt(LocalDateTime.of(2026, 10, 28, 9, 0))
                .completedAt(LocalDateTime.of(2026, 10, 28, 10, 25)).build());
        list.add(PilotTaskDto.builder().no(11L).address("구즉동 5-3").region("유성구 구즉동").areaSize(2200.0)
                .status("COMPLETED").pilotName("김민수").chemicalName("살균제 A").usedAmount(2.6)
                .assignedAt(LocalDateTime.of(2026, 10, 27, 13, 0))
                .completedAt(LocalDateTime.of(2026, 10, 27, 14, 20)).build());
        list.add(PilotTaskDto.builder().no(12L).address("노은동 30-1").region("유성구 노은동").areaSize(3100.0)
                .status("COMPLETED").pilotName("이서연").chemicalName("살충제 B").usedAmount(3.5)
                .assignedAt(LocalDateTime.of(2026, 10, 20, 8, 30))
                .completedAt(LocalDateTime.of(2026, 10, 20, 10, 5)).build());
        list.add(PilotTaskDto.builder().no(13L).address("전민동 66").region("유성구 전민동").areaSize(2700.0)
                .status("COMPLETED").pilotName("이서연").chemicalName("살균제 A").usedAmount(3.0)
                .assignedAt(LocalDateTime.of(2026, 9, 30, 9, 0))
                .completedAt(LocalDateTime.of(2026, 9, 30, 10, 40)).build());
        return list;
    }
}