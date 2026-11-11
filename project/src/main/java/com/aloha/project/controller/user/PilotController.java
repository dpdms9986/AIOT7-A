
package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.common.Pagination;
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
import com.aloha.project.repository.lands.LandRepository;
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
    private final LandRepository landRepository;

    /* =========================================================
       조종사 대시보드 - 내 작업 목록 + 지도
       ========================================================= */
    @Transactional(readOnly = true)
    @GetMapping("/dashboard")
    public String getDashboard(
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        if (customUser == null) return "redirect:/login";
        Users pilot = getLoginPilot(customUser);
        // 로그인 조종사의 소속 방제단 정보 전달
        if (pilot.getTeam() != null) {
         model.addAttribute("myTeamChannel", pilot.getTeam().getInviteCode());
         model.addAttribute("myTeamName", pilot.getTeam().getTeamName());
        } else {
         model.addAttribute("myTeamChannel", null);
         model.addAttribute("myTeamName", null);
        }
        
        // 로그인한 조종사 본인 이름 전달
        model.addAttribute("myPilotName", pilot.getName());
        
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
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        if (customUser == null) return "redirect:/login";
        Users pilot = getLoginPilot(customUser);

        // 로그인 조종사 본인의 전체 작업 이력
        List<PilotTaskDto> all = workAssignmentRepository.findByPilot_No(pilot.getNo())
                .stream()
                .map(this::toPilotTaskDto)
                .toList();

        // 1) 탭으로 거르기
        List<PilotTaskDto> tasks = all.stream()
                .filter(t -> switch (filter) {
                    case "waiting" -> "ASSIGNED".equals(t.getStatus());
                    case "active" -> "IN_PROGRESS".equals(t.getStatus());
                    case "done" -> t.isDone();
                    default -> true;
                })
                .toList();

        // 1-2) 페이지네이션 (탭으로 거른 목록을 10개씩 자름)
        final int pageSize = 10;
        Pagination pagination = new Pagination(Math.max(page, 1), pageSize, 10, tasks.size());
        if (pagination.getPage() > pagination.getLast()) {
            pagination = new Pagination(pagination.getLast(), pageSize, 10, tasks.size());
        }
        int fromIdx = (int) Math.min(pagination.getIndex(), tasks.size());
        int toIdx   = Math.min(fromIdx + pageSize, tasks.size());
        List<PilotTaskDto> pagedTasks = tasks.subList(fromIdx, toIdx);

        // 2) 탭 개수 + 완료 면적
        long waitingCount = all.stream()
                .filter(t -> "ASSIGNED".equals(t.getStatus()))
                .count();

        long activeCount = all.stream()
                .filter(t -> "IN_PROGRESS".equals(t.getStatus()))
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

        // 3-1) 진행 중(IN_PROGRESS) 작업에서 이미 사용 중인 드론/약제 번호 모으기
        Set<Long> usedDroneNos = new HashSet<>();
        Set<Long> usedChemicalNos = new HashSet<>();

        workAssignmentRepository.findByPilot_No(pilot.getNo())
                .stream()
                .filter(a -> "IN_PROGRESS".equals(a.getStatus()))
                .forEach(a -> workAssignmentResourceRepository
                        .findByWorkAssignment_No(a.getNo())
                        .ifPresent(r -> {
                            usedDroneNos.add(r.getDrone().getNo());
                            usedChemicalNos.add(r.getChemical().getNo());
                        }));

        // 4) 로그인 조종사 본인 소유의 사용 가능한 드론 목록 (사용 중인 드론 제외)
        List<OptionDto> drones = droneRepository.findByUser_No(pilot.getNo())
                .stream()
                .filter(drone -> "AVAILABLE".equals(drone.getStatus()))
                .filter(drone -> !usedDroneNos.contains(drone.getNo()))
                .map(this::toOptionDto)
                .toList();

        // 5) 로그인 조종사 본인이 담당하는 약제 목록 (사용 중인 약제 제외)
        List<OptionDto> chemicals = chemicalRepository.findByUser_No(pilot.getNo())
                .stream()
                .filter(chemical -> !usedChemicalNos.contains(chemical.getNo()))
                .map(chemical -> new OptionDto(
                        chemical.getNo(),
                        chemical.getChemicalName()))
                .toList();

        // 6) 화면으로 보낼 값 (기존 Model 변수명 유지)
        model.addAttribute("filter", filter);
        model.addAttribute("tasks", pagedTasks);
        model.addAttribute("pagination", pagination);
        model.addAttribute("allCount", all.size());
        model.addAttribute("waitingCount", waitingCount);
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

                
        /**
         * 작업 삭제 처리
         * POST /pilot/history/{no}/cancel
         */
        @Transactional
        @PostMapping("/history/{no}/cancel")
        public String cancelWork(@PathVariable("no") Long no,
                                @AuthenticationPrincipal CustomUser customUser,
                                RedirectAttributes redirectAttributes) {
                if (customUser == null) return "redirect:/login";
                Users pilot = getLoginPilot(customUser);

                WorkAssignments task = workAssignmentRepository.findById(no).orElse(null);
                if (task == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 작업입니다.");
                return "redirect:/pilot/history";
                }

                // 1. 본인 작업인지 권한 확인
                if (task.getPilot() == null || !task.getPilot().getNo().equals(pilot.getNo())) {
                redirectAttributes.addFlashAttribute("errorMessage", "본인에게 배정된 작업만 삭제할 수 있습니다.");
                return "redirect:/pilot/history";
                }

                // 2. 이미 완료된 작업은 취소 불가 방어
                if ("COMPLETED".equals(task.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "이미 완료된 작업은 취소 불가합니다.");
                return "redirect:/pilot/history";
                }

                // 3. 선택했던 드론/약제 매핑 삭제 (다시 선택하도록 초기화)
                workAssignmentResourceRepository.findByWorkAssignment_No(task.getNo())
                        .ifPresent(workAssignmentResourceRepository::delete);

                // 4. 해당 조종사의 작업 상태를 다시 ASSIGNED(대기)로 변경
                task.setStatus("ASSIGNED");
                task.setStartedAt(null);
                workAssignmentRepository.save(task);

                // 5. 같은 농지에 배정된 전체 조종사의 작업 상태를 기준으로 농지 상태 갱신
                if (task.getLand() != null) {
                updateLandStatus(task.getLand());
                }

                log.info("작업 취소 및 대기 복구 완료: taskNo={}, pilot={}", no, pilot.getName());
                redirectAttributes.addFlashAttribute("message", "작업이 취소되어 다시 '대기' 상태로 변경됩니다.");

                // 작업 상세 번호 유지하며 리다이렉트
                return "redirect:/pilot/history?no=" + no;
        }

        /**
         * 같은 농지에 배정된 전체 작업 상태를 기준으로 농지 상태 갱신
         */
        private void updateLandStatus(Lands land) {

                List<WorkAssignments> landAssignments =
                        workAssignmentRepository.findByLand(land);

                // 모든 조종사의 작업이 완료되었는지 확인
                boolean allCompleted = !landAssignments.isEmpty()
                        && landAssignments.stream()
                                .allMatch(work -> "COMPLETED".equals(work.getStatus()));

                // 작업을 시작했거나 완료한 조종사가 있는지 확인
                boolean hasStarted = landAssignments.stream()
                        .anyMatch(work ->
                                "IN_PROGRESS".equals(work.getStatus())
                                || "COMPLETED".equals(work.getStatus()));

                if (allCompleted) {
                land.updateStatus("COMPLETED");
                } else if (hasStarted) {
                land.updateStatus("IN_PROGRESS");
                } else {
                land.updateStatus("ASSIGNED");
                }

                landRepository.save(land);
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
                .address(land.getAddress())
                .latitude(land.getLatitude())
                .longitude(land.getLongitude())
                .areaSize(land.getAreaSize())
                .status(assignment.getStatus())
                .pilotName(assignment.getPilot() == null
                        ? null : assignment.getPilot().getName())
                .assignedAt(assignment.getCreatedAt())
                .polygonCoordinates(land != null ? land.getPolygonCoordinates() : null)
                .flightPath(assignment.getFlightPath())
                .build();
    }

        /** 작업 배정 엔티티 → 작업 이력 DTO */
        private PilotTaskDto toPilotTaskDto(WorkAssignments assignment) {
        Lands land = assignment.getLand();

        // 작업 시작 시 선택한 드론과 약제 정보 조회
        WorkAssignmentResources resources = workAssignmentResourceRepository
                .findByWorkAssignment_No(assignment.getNo())
                .orElse(null);

        return PilotTaskDto.builder()
                .no(assignment.getNo())
                .address(land.getAddress())
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
                .flightPath(assignment.getFlightPath())
                .lastLatitude(assignment.getLastLatitude())
                .lastLongitude(assignment.getLastLongitude())
                .lastLocationAt(assignment.getLastLocationAt())
                .build();
        }

    private OptionDto toOptionDto(Drones drone) {
        return new OptionDto(drone.getNo(), drone.getModelName());
    }

    /**
     * 조종사 비행 중 궤적 저장 또는 작업 일시 중단 (파일럿 1)
     * POST /pilot/tasks/{no}/save-path
     */
    @Transactional
    @PostMapping("/tasks/{no}/save-path")
    @ResponseBody
    public ResponseEntity<?> saveFlightPath(@PathVariable("no") Long assignmentNo,
                                            @RequestParam("flightPath") String flightPath,
																						@AuthenticationPrincipal CustomUser customUser) 
			{
			if (customUser == null) return ResponseEntity.status(401).build();

			WorkAssignments task = workAssignmentRepository.findById(assignmentNo).orElse(null);
			if (task == null) return ResponseEntity.badRequest().body("작업을 찾을 수 없습니다.");

			// 궤적 좌표 JSON 업데이트 (예: "[[23.1233, 123.1223].[36.123, 127.1212]]")
			task.setFlightPath(flightPath);
			workAssignmentRepository.save(task);

			return ResponseEntity.ok(true);
			}
			
}