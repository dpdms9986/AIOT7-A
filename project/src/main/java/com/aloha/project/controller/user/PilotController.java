package com.aloha.project.controller.user;

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
 * - 데이터는 TODO(백엔드) 부분을 서비스 호출로 연결 예정 (지금은 빈 목록)
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

        List<LandDto> myTasks = new ArrayList<>();      // TODO(백엔드): 내 작업 목록
        List<LandDto> otherFarms = new ArrayList<>();   // TODO(백엔드): 팀의 다른 농지

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
        List<PilotTaskDto> all = new ArrayList<>();

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
        double doneUsed = done.stream().mapToDouble(t -> {
            try {
                return (t.getUsedAmount() != null) ? Double.parseDouble(t.getUsedAmount()) : 0.0;
            } catch (Exception e) {
                return 0.0;
            }
        }).sum();

        // 3) 상세로 보여줄 작업: 주소의 no → 없으면 진행 중 → 없으면 첫 번째
        PilotTaskDto selected = Optional.ofNullable(no)
                .flatMap(n -> all.stream().filter(t -> t.getNo().equals(n)).findFirst())
                .or(() -> all.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).findFirst())
                .or(() -> all.stream().findFirst())
                .orElse(null);

        // 4) 드론, 약제 선택 목록
        List<OptionDto> drones = new ArrayList<>();      // TODO(백엔드): 내 방제단 드론
        List<OptionDto> chemicals = new ArrayList<>();   // TODO(백엔드): 약제 목록

        // 5) 화면으로 보낼 값
        model.addAttribute("filter", filter);
        model.addAttribute("tasks", tasks);
        model.addAttribute("allCount", all.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("doneCount", done.size());
        model.addAttribute("doneArea", doneArea);
        model.addAttribute("doneUsed", doneUsed);
        model.addAttribute("selected", selected);
        model.addAttribute("drones", drones);
        model.addAttribute("chemicals", chemicals);

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
}
