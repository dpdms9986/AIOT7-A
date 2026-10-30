package com.aloha.project.controller.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.dto.LandDto;
import com.aloha.project.dto.OptionDto;
import com.aloha.project.dto.PilotTaskDto;
import com.aloha.project.dto.WorkStartRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 조종사(PILOT) 화면 컨트롤러
 * - 주소가 /pilot 으로 시작하는 요청을 처리
 * - 지금은 샘플 데이터 사용 → TODO(백엔드) 부분을 서비스 호출로 교체 예정
 */
@Slf4j
@Controller
@RequestMapping("/pilot")
public class PilotController {

    /* =========================================================
       조종사 대시보드 - 내 작업 목록 + 지도
       ========================================================= */
    @GetMapping("/dashboard")
    public String getDashboard(Model model) {

        List<LandDto> myTasks = sampleMyTasks();        // TODO(백엔드)
        List<LandDto> otherFarms = sampleOtherFarms();  // TODO(백엔드)

        Long selectedNo = myTasks.stream()
                .filter(t -> "IN_PROGRESS".equals(t.getStatus()))
                .findFirst()
                .or(() -> myTasks.stream().findFirst())
                .map(LandDto::getNo)
                .orElse(null);

        model.addAttribute("myTasks", myTasks);
        model.addAttribute("otherFarms", otherFarms);
        model.addAttribute("selectedNo", selectedNo);

        return "page/pilot/dashboard";
    }

    /* =========================================================
       작업 이력 - 목록 + 탭 + 선택한 작업 상세
       예) /pilot/history                   → 전체, 진행 중 작업 자동 선택
           /pilot/history?filter=done&no=10 → 완료 탭, 10번 작업 상세
       ========================================================= */
    @GetMapping("/history")
    public String getHistory(
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            @RequestParam(value = "no", required = false) Long no,
            Model model) {

        // TODO(백엔드): workAssignmentService.getMyHistory(로그인 조종사)
        List<PilotTaskDto> all = sampleHistory();

        // 1) 탭으로 거르기
        List<PilotTaskDto> tasks = all.stream()
                .filter(t -> switch (filter) {
                    case "active" -> !t.isDone();
                    case "done"   -> t.isDone();
                    default       -> true;
                })
                .toList();

        // 2) 탭 개수 + 완료 요약
        long activeCount = all.stream().filter(t -> !t.isDone()).count();
        List<PilotTaskDto> done = all.stream().filter(PilotTaskDto::isDone).toList();
        double doneArea = done.stream().mapToDouble(t -> t.getAreaSize() == null ? 0 : t.getAreaSize()).sum();
        double doneUsed = done.stream().mapToDouble(t -> t.getUsedAmount() == null ? 0 : t.getUsedAmount()).sum();

        // 3) 상세로 보여줄 작업: 주소의 no → 없으면 진행 중 → 없으면 첫 번째
        PilotTaskDto selected = Optional.ofNullable(no)
                .flatMap(n -> all.stream().filter(t -> t.getNo().equals(n)).findFirst())
                .or(() -> all.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).findFirst())
                .or(() -> all.stream().findFirst())
                .orElse(null);

        // 4) 화면으로 보낼 값
        model.addAttribute("filter", filter);
        model.addAttribute("tasks", tasks);
        model.addAttribute("allCount", all.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("doneCount", done.size());
        model.addAttribute("doneArea", doneArea);
        model.addAttribute("doneUsed", doneUsed);
        model.addAttribute("selected", selected);
        model.addAttribute("drones", sampleDrones());         // TODO(백엔드): 내 방제단 드론
        model.addAttribute("chemicals", sampleChemicals());   // TODO(백엔드): 약제 목록

        return "page/pilot/history";
    }

    /**
     * 작업 시작 (대기 → 진행 중)
     * POST /pilot/history/{no}/start  (droneNo, chemicalNo, sprayRate)
     */
    @PostMapping("/history/{no}/start")
    public String startWork(@PathVariable("no") Long no,
                            @ModelAttribute WorkStartRequest req,
                            RedirectAttributes redirectAttributes) {
        log.info("작업 시작 요청: no={}, {}", no, req);

        // 돌아갈 주소에 ?no=번호 붙이기 → 보던 작업이 다시 선택됨
        redirectAttributes.addAttribute("no", no);

        // 서버 검증
        if (req.getDroneNo() == null || req.getChemicalNo() == null
                || req.getSprayRate() == null || req.getSprayRate() <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "드론, 약제, 살포량을 모두 입력하세요.");
            return "redirect:/pilot/history";
        }

        // TODO(백엔드): workAssignmentService.start(로그인 조종사, no, req)
        //   ※ 내 작업인지, ASSIGNED 상태인지 확인 → IN_PROGRESS 로 변경, 시작 시각 저장
        redirectAttributes.addFlashAttribute("message", "작업을 시작했습니다. (샘플)");
        return "redirect:/pilot/history";
    }

    /**
     * 작업 완료 보고 (진행 중 → 완료)
     * POST /pilot/history/{no}/complete  (usedAmount)
     */
    @PostMapping("/history/{no}/complete")
    public String completeWork(@PathVariable("no") Long no,
                               @RequestParam("usedAmount") Double usedAmount,
                               RedirectAttributes redirectAttributes) {
        log.info("완료 보고 요청: no={}, usedAmount={}", no, usedAmount);

        redirectAttributes.addAttribute("no", no);

        if (usedAmount == null || usedAmount <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "실제 약제 사용량을 입력하세요.");
            return "redirect:/pilot/history";
        }

        // TODO(백엔드): workAssignmentService.complete(로그인 조종사, no, usedAmount)
        //   ※ 내 작업인지, IN_PROGRESS 상태인지 확인 → COMPLETED 로 변경, 완료 시각 저장
        redirectAttributes.addFlashAttribute("message", "작업 완료를 보고했습니다. (샘플)");
        return "redirect:/pilot/history";
    }


    /* =========================================================
       샘플 데이터 (백엔드 연결 전까지만 사용)
       - 로그인한 조종사를 "김민수"라고 가정
       ========================================================= */
    private List<LandDto> sampleMyTasks() {
        List<LandDto> list = new ArrayList<>();
        list.add(LandDto.builder().no(3L).city("대전광역시").region("유성구").dong("전민동")
                .address("전민동 31-7").latitude(36.3890).longitude(127.4071).areaSize(4800.0)
                .status("IN_PROGRESS").pilotName("김민수")
                .assignedAt(LocalDateTime.of(2026, 10, 29, 8, 0)).build());
        list.add(LandDto.builder().no(8L).city("대전광역시").region("유성구").dong("전민동")
                .address("전민동 50-2").latitude(36.3955).longitude(127.4098).areaSize(2900.0)
                .status("ASSIGNED").pilotName("김민수")
                .assignedAt(LocalDateTime.of(2026, 10, 29, 8, 0)).build());
        list.add(LandDto.builder().no(9L).city("대전광역시").region("유성구").dong("구즉동")
                .address("구즉동 21").latitude(36.4188).longitude(127.3925).areaSize(3600.0)
                .status("ASSIGNED").pilotName("김민수")
                .assignedAt(LocalDateTime.of(2026, 10, 29, 10, 15)).build());
        list.add(LandDto.builder().no(10L).city("대전광역시").region("유성구").dong("전민동")
                .address("전민동 8-11").latitude(36.3874).longitude(127.4005).areaSize(1500.0)
                .status("COMPLETED").pilotName("김민수")
                .assignedAt(LocalDateTime.of(2026, 10, 28, 9, 0)).build());
        return list;
    }

    private List<LandDto> sampleOtherFarms() {
        List<LandDto> list = new ArrayList<>();
        list.add(LandDto.builder().no(1L).address("전민동 15-2")
                .latitude(36.3912).longitude(127.4035).areaSize(3300.0).status("READY").build());
        list.add(LandDto.builder().no(5L).address("전민동 42-1")
                .latitude(36.3931).longitude(127.4012).areaSize(2600.0).status("READY").build());
        return list;
    }

    private List<PilotTaskDto> sampleHistory() {
        List<PilotTaskDto> list = new ArrayList<>();
        list.add(PilotTaskDto.builder().no(3L).address("전민동 31-7").areaSize(4800.0)
                .status("IN_PROGRESS").assignedAt(LocalDateTime.of(2026, 10, 29, 8, 0))
                .droneName("DJI T40-01").chemicalName("살균제 A").sprayRate(12.0)
                .startedAt(LocalDateTime.of(2026, 10, 29, 9, 10)).build());
        list.add(PilotTaskDto.builder().no(8L).address("전민동 50-2").areaSize(2900.0)
                .status("ASSIGNED").assignedAt(LocalDateTime.of(2026, 10, 29, 8, 0)).build());
        list.add(PilotTaskDto.builder().no(9L).address("구즉동 21").areaSize(3600.0)
                .status("ASSIGNED").assignedAt(LocalDateTime.of(2026, 10, 29, 10, 15)).build());
        list.add(PilotTaskDto.builder().no(10L).address("전민동 8-11").areaSize(1500.0)
                .status("COMPLETED").assignedAt(LocalDateTime.of(2026, 10, 28, 9, 0))
                .droneName("DJI T40-01").chemicalName("살충제 B").sprayRate(10.0)
                .startedAt(LocalDateTime.of(2026, 10, 28, 9, 40))
                .usedAmount(1.8).completedAt(LocalDateTime.of(2026, 10, 28, 10, 25)).build());
        list.add(PilotTaskDto.builder().no(11L).address("구즉동 5-3").areaSize(2200.0)
                .status("COMPLETED").assignedAt(LocalDateTime.of(2026, 10, 27, 13, 0))
                .droneName("DJI T40-01").chemicalName("살균제 A").sprayRate(12.0)
                .startedAt(LocalDateTime.of(2026, 10, 27, 13, 30))
                .usedAmount(2.6).completedAt(LocalDateTime.of(2026, 10, 27, 14, 20)).build());
        return list;
    }

    private List<OptionDto> sampleDrones() {
        return List.of(new OptionDto(1L, "DJI T40-01 (40L)"),
                       new OptionDto(2L, "DJI T30-02 (30L)"));
    }

    private List<OptionDto> sampleChemicals() {
        return List.of(new OptionDto(1L, "살균제 A (아족시스트로빈)"),
                       new OptionDto(2L, "살충제 B (클로란트라닐리프롤)"));
    }
}