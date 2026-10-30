
package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
import com.aloha.project.domain.tasks.WorkAssignmentResources;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.LandDto;
import com.aloha.project.dto.OptionDto;
import com.aloha.project.dto.PilotTaskDto;
import com.aloha.project.dto.WorkStartRequest;
import com.aloha.project.repository.tasks.ChemicalRepository;
import com.aloha.project.repository.tasks.DroneRepository;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.tasks.WorkAssignmentService;
import com.aloha.project.repository.tasks.WorkAssignmentResourceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 조종사(PILOT) 화면 컨트롤러
 * - 주소가 /pilot 으로 시작하는 요청을 처리
 * - 화면과 URL, Model 변수명은 프론트엔드 정의를 유지하고 DB 데이터만 연결
 */
@Slf4j
@Controller
@RequestMapping("/pilot")
@RequiredArgsConstructor
public class PilotController {

    private final UserRepository userRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final DroneRepository droneRepository;
    private final ChemicalRepository chemicalRepository;
    private final WorkAssignmentService workAssignmentService;
    private final WorkAssignmentResourceRepository workAssignmentResourceRepository;

    /* =========================================================
       조종사 대시보드 - 내 작업 목록 + 지도
       ========================================================= */
    @Transactional(readOnly = true)
    @GetMapping("/dashboard")
    public String getDashboard(
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        Users pilot = getLoginPilot(customUser);

        // 현재 로그인한 조종사에게 배정된 작업
        List<LandDto> myTasks = workAssignmentRepository.findByPilot_No(pilot.getNo())
                .stream()
                .map(this::toLandDto)
                .toList();

        // 같은 방제단 소속의 다른 조종사들이 현재 작업 중인 농지
        List<LandDto> otherFarms = new ArrayList<>();

        if (pilot.getTeam() != null) {
            otherFarms = workAssignmentRepository
                    .findByPilot_TeamAndStatus(pilot.getTeam(), "IN_PROGRESS")
                    .stream()
                    .filter(assignment ->
                            !assignment.getPilot().getNo().equals(pilot.getNo()))
                    .map(this::toLandDto)
                    .toList();
        }

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
       ========================================================= */
    @Transactional(readOnly = true)
    @GetMapping("/history")
    public String getHistory(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            @RequestParam(value = "no", required = false) Long no,
            Model model) {

        Users pilot = getLoginPilot(customUser);

        // 로그인 조종사 본인의 전체 작업 이력
        List<PilotTaskDto> all = workAssignmentRepository.findByPilot_No(pilot.getNo())
                .stream()
                .map(this::toPilotTaskDto)
                .toList();

        // 1) 탭으로 거르기
        List<PilotTaskDto> tasks = all.stream()
                .filter(t -> switch (filter) {
                    case "active" -> !t.isDone();
                    case "done" -> t.isDone();
                    default -> true;
                })
                .toList();

        // 2) 탭 개수 + 완료 면적
        long activeCount = all.stream()
                .filter(t -> !t.isDone())
                .count();

        List<PilotTaskDto> done = all.stream()
                .filter(PilotTaskDto::isDone)
                .toList();

        double doneArea = done.stream()
                .mapToDouble(t -> t.getAreaSize() == null ? 0 : t.getAreaSize())
                .sum();

        // 3) 상세로 보여줄 작업: 주소의 no → 없으면 진행 중 → 없으면 첫 번째
        PilotTaskDto selected = Optional.ofNullable(no)
                .flatMap(n -> all.stream()
                        .filter(t -> t.getNo().equals(n))
                        .findFirst())
                .or(() -> all.stream()
                        .filter(t -> "IN_PROGRESS".equals(t.getStatus()))
                        .findFirst())
                .or(() -> all.stream().findFirst())
                .orElse(null);

        // 4) 내 방제단에서 사용 가능한 드론 목록
        List<OptionDto> drones = pilot.getTeam() == null
                ? new ArrayList<>()
                : droneRepository.findByUser_TeamAndStatus(pilot.getTeam(), "AVAILABLE")
                        .stream()
                        .map(this::toOptionDto)
                        .toList();

        // 5) 내 방제단에 등록된 약제 목록
        List<OptionDto> chemicals = pilot.getTeam() == null
                ? new ArrayList<>()
                : chemicalRepository.findByTeam_No(pilot.getTeam().getNo())
                        .stream()
                        .map(chemical -> new OptionDto(
                                chemical.getNo(),
                                chemical.getChemicalName()))
                        .toList();

        // 6) 화면으로 보낼 값 (기존 Model 변수명 유지)
        model.addAttribute("filter", filter);
        model.addAttribute("tasks", tasks);
        model.addAttribute("allCount", all.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("doneCount", done.size());
        model.addAttribute("doneArea", doneArea);
        model.addAttribute("selected", selected);
        model.addAttribute("drones", drones);
        model.addAttribute("chemicals", chemicals);

        return "page/pilot/history";
    }

        /**
         * 작업 시작 (대기 → 진행 중)
         * POST /pilot/history/{no}/start
         */
        @PostMapping("/history/{no}/start")
        public String startWork(
                @PathVariable("no") Long no,
                @ModelAttribute WorkStartRequest req,
                @AuthenticationPrincipal CustomUser customUser,
                RedirectAttributes redirectAttributes) {

        log.info("작업 시작 요청: no={}, {}", no, req);

        // 작업 상세 번호 유지
        redirectAttributes.addAttribute("no", no);

        // 드론과 약제 선택 여부 확인
        if (req.getDroneNo() == null || req.getChemicalNo() == null) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "드론과 약제를 선택하세요."
                );
                return "redirect:/pilot/history";
        }

        try {
                Users pilot = getLoginPilot(customUser);

                workAssignmentService.startAssignment(
                        no,
                        pilot.getNo(),
                        req.getDroneNo(),
                        req.getChemicalNo()
                );

                redirectAttributes.addFlashAttribute(
                        "message",
                        "작업을 시작했습니다."
                );

        } catch (RuntimeException e) {
                log.warn("작업 시작 실패: no={}, message={}", no, e.getMessage());

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        e.getMessage()
                );
        }

        return "redirect:/pilot/history";
        }

        /**
         * 작업 완료 보고 (진행 중 → 완료)
         * POST /pilot/history/{no}/complete
         */
        @PostMapping("/history/{no}/complete")
        public String completeWork(
        @PathVariable("no") Long no,
        @AuthenticationPrincipal CustomUser customUser,
        RedirectAttributes redirectAttributes) {

        log.info("완료 보고 요청: no={}", no);

        // 작업 상세 번호 유지
        redirectAttributes.addAttribute("no", no);

        try {
        Users pilot = getLoginPilot(customUser);

        workAssignmentService.updateAssignmentStatus(
                no,
                "COMPLETED",
                pilot.getNo()
        );

        redirectAttributes.addFlashAttribute(
                "message",
                "작업 완료 처리되었습니다."
        );

        } catch (RuntimeException e) {
        log.warn("작업 완료 실패: no={}, message={}", no, e.getMessage());

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                e.getMessage()
        );
        }

        return "redirect:/pilot/history";
        }

    /** 로그인한 조종사 엔티티를 DB에서 다시 조회 */
    private Users getLoginPilot(CustomUser customUser) {
        if (customUser == null || customUser.getUser() == null) {
            throw new IllegalStateException("조종사 로그인이 필요합니다.");
        }

        Users pilot = userRepository.findById(customUser.getUser().getNo())
                .orElseThrow(() ->
                        new IllegalArgumentException("로그인 사용자를 찾을 수 없습니다."));

        if (!"PILOT".equals(pilot.getRole())) {
            throw new IllegalStateException("조종사 계정만 접근할 수 있습니다.");
        }

        return pilot;
    }

    /** 작업 배정 엔티티 → 대시보드 농지 DTO */
    private LandDto toLandDto(WorkAssignments assignment) {
        Lands land = assignment.getLand();

        return LandDto.builder()
                .no(land.getNo())
                .city(land.getRegion() == null ? null : land.getRegion().getSido())
                .region(land.getRegion() == null ? null : land.getRegion().getSigungu())
                .dong(land.getRegion() == null ? null : land.getRegion().getEupmyeondong())
                .address(land.getAddress())
                .latitude(land.getLatitude())
                .longitude(land.getLongitude())
                .areaSize(land.getAreaSize())
                .status(assignment.getStatus())
                .pilotName(assignment.getPilot() == null
                        ? null : assignment.getPilot().getName())
                .assignedAt(assignment.getCreatedAt())
                .build();
    }

        /** 작업 배정 엔티티 → 작업 이력 DTO */
        private PilotTaskDto toPilotTaskDto(WorkAssignments assignment) {
        Lands land = assignment.getLand();

        String region = null;

        if (land.getRegion() != null) {
                region = String.join(" ",
                        land.getRegion().getSigungu() == null
                                ? "" : land.getRegion().getSigungu(),
                        land.getRegion().getEupmyeondong() == null
                                ? "" : land.getRegion().getEupmyeondong())
                        .trim();
        }

        // 작업 시작 시 선택한 드론과 약제 정보 조회
        WorkAssignmentResources resources = workAssignmentResourceRepository
                .findByWorkAssignment_No(assignment.getNo())
                .orElse(null);

        return PilotTaskDto.builder()
                .no(assignment.getNo())
                .address(land.getAddress())
                .region(region)
                .areaSize(land.getAreaSize())
                .status(assignment.getStatus())
                .pilotName(assignment.getPilot() == null
                        ? null : assignment.getPilot().getName())
                .assignedAt(assignment.getCreatedAt())
                .startedAt(assignment.getStartedAt())
                .completedAt(assignment.getCompletedAt())
                .droneName(resources == null
                        ? null : resources.getDrone().getModelName())
                .chemicalName(resources == null
                        ? null : resources.getChemical().getChemicalName())
                .build();
        }

    private OptionDto toOptionDto(Drones drone) {
        return new OptionDto(drone.getNo(), drone.getModelName());
    }
}