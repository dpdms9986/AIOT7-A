package com.aloha.project.controller.user;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import com.aloha.project.domain.tasks.DispatchBatches;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.AssignRequest;
import com.aloha.project.dto.LandCreateRequest;
import com.aloha.project.dto.LandDto;
import com.aloha.project.dto.PilotTaskDto;
import com.aloha.project.dto.TeamMemberDto;
import com.aloha.project.repository.lands.LandRepository;
import com.aloha.project.repository.tasks.DispatchBatchRepository;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 방제단장(LEADER) 화면 컨트롤러 (실제 DB 연동 완료)
 */
@Slf4j
@Controller
@RequestMapping("/leader")
@RequiredArgsConstructor
public class LeaderController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserRepository userRepository;
    private final LandRepository landRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final DispatchBatchRepository dispatchBatchRepository;
    private final ObjectMapper objectMapper;

    /* =========================================================
       작업 배정 (탭 3개: assign / status / history)
       ========================================================= */
    @Transactional(readOnly = true)
    @GetMapping("/task-assignment")
    public String getTaskAssignment(
            @RequestParam(value = "tab",     defaultValue = "assign") String tab,
            @RequestParam(value = "region",  defaultValue = "") String region,
            @RequestParam(value = "landNo",  required = false) Long landNo,
            @RequestParam(value = "status",  defaultValue = "") String status,
            @RequestParam(value = "pilot",   defaultValue = "") String pilot,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "hRegion", defaultValue = "") String hRegion,
            @RequestParam(value = "hPilot",  defaultValue = "") String hPilot,
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        if (customUser == null) return "redirect:/login";

        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;
        final String inviteCode = (myTeam != null && myTeam.getInviteCode() != null) ? myTeam.getInviteCode() : "";

        // [DB 연동 1] 전체 농지 목록 조회 (LandDto 변환)
        List<LandDto> allLands = landRepository.findAll().stream()
                .map(LandDto::from)
                .toList();

        // [DB 연동 2] 소속 조종사 목록 조회 (TeamMemberDto 변환)
        List<TeamMemberDto> pilots = new ArrayList<>();
        if (!inviteCode.isEmpty()) {
            pilots = userRepository.findAll().stream()
                    .filter(u -> u.getTeam() != null
                              && inviteCode.equals(u.getTeam().getInviteCode())
                              && !"LEADER".equals(u.getRole()))
                    .map(TeamMemberDto::from)
                    .toList();
        }

        /* ----- [배정 탭] 미배정(READY) 농지 + 지역 검색 ----- */
        String[] words = region.trim().isEmpty() ? new String[0] : region.trim().split("\\s+");
        List<LandDto> readyLands = allLands.stream()
                .filter(l -> "READY".equals(l.getStatus()))
                .filter(l -> Arrays.stream(words)
                        .allMatch(w -> ((l.getFullRegion() != null ? l.getFullRegion() : "") + " " + l.getAddress()).contains(w)))
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
                .filter(l -> l.getPilotName() != null)
                .collect(Collectors.groupingBy(LandDto::getPilotName, LinkedHashMap::new, Collectors.counting()));

        /* ----- [이력 탭] 완료된 작업 + 기간/지역/조종사 검색 ----- */
        boolean dateError = from != null && to != null && from.isAfter(to);
        List<PilotTaskDto> history = dateError ? List.of() : filterHistory(from, to, hRegion, hPilot);
        double hArea = history.stream().mapToDouble(h -> h.getAreaSize() == null ? 0 : h.getAreaSize()).sum();
        double hUsed = history.stream().mapToDouble(h -> {
            try {
                return (h.getUsedAmount() != null) ? Double.parseDouble(h.getUsedAmount()) : 0.0;
            } catch (Exception e) {
                return 0.0; // "양호" 같은 글자일 경우 0.0 처리
            }
        }).sum();
        List<String> historyPilots = pilots.stream().map(TeamMemberDto::getName).toList();

        /* ----- 화면으로 보낼 값 ----- */
        model.addAttribute("tab", tab);
        model.addAttribute("readyLands", readyLands);
        model.addAttribute("pilots", pilots);
        model.addAttribute("region", region);
        model.addAttribute("landNo", landNo);

        model.addAttribute("assigned", assigned);
        model.addAttribute("assignedTotal", allAssigned.size());
        model.addAttribute("countByPilot", countByPilot);
        model.addAttribute("status", status);
        model.addAttribute("pilot", pilot);

        model.addAttribute("history", history);
        model.addAttribute("hArea", hArea);
        model.addAttribute("historyPilots", historyPilots);
        model.addAttribute("dateError", dateError);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("hRegion", hRegion);
        model.addAttribute("hPilot", hPilot);

        return "page/leader/task-assignment";
    }

    /**
     * 작업 이력 CSV 다운로드
     */
    @GetMapping("/task-assignment/history.csv")
    public ResponseEntity<byte[]> downloadHistoryCsv(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "hRegion", defaultValue = "") String hRegion,
            @RequestParam(value = "hPilot",  defaultValue = "") String hPilot) {

        List<PilotTaskDto> rows = filterHistory(from, to, hRegion, hPilot);

        StringBuilder sb = new StringBuilder("\uFEFF");   // 엑셀 한글 깨짐 방지
        sb.append("완료 일시,주소,행정구역,면적(㎡),담당 조종사,배정 일시,사용 약제\r\n");
        for (PilotTaskDto r : rows) {
            sb.append(String.join(",",
                    csv(r.getCompletedAt() == null ? "" : r.getCompletedAt().format(DT)),
                    csv(r.getAddress()),
                    csv(r.getRegion()),
                    csv(r.getAreaSize()),
                    csv(r.getPilotName()),
                    csv(r.getAssignedAt() == null ? "" : r.getAssignedAt().format(DT)),
                    csv(r.getChemicalName())))
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
     * 작업 배정 (DB 저장)
     * - 구역별 배정: 농지 1건(또는 지도에서 직접 그린 영역)을 N조각으로 나눠 조종사별로 배정
     * - 일반 배정: 여러 농지를 조종사별 필지 수(quotas)대로 배정
     */
    @PostMapping("/task-assignment")
    public String assign(@ModelAttribute AssignRequest req,
                         @AuthenticationPrincipal CustomUser customUser,
                         RedirectAttributes redirectAttributes) {
        log.info("작업 배정 요청: 단장={}, req={}", customUser.getUsername(), req);

        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;

        /* ---------- 구역별(조각별) 배정 ---------- */
        List<PieceAssignment> pieceList = parsePieces(req.getPieceAssignmentsJson());
        if (!pieceList.isEmpty()) {
            if (leader == null || myTeam == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "소속 방제단 정보를 찾을 수 없습니다.");
                return "redirect:/leader/task-assignment";
            }
            return assignByPieces(req, pieceList, leader, myTeam, redirectAttributes);
        }

        /* ---------- 일반 배정 (여러 농지 + 조종사별 필지 수) ---------- */
        if (req.getLandNos() == null || req.getLandNos().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "배정할 농지를 선택하세요.");
            return "redirect:/leader/task-assignment";
        }
        if (req.totalQuota() != req.getLandNos().size()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "조종사별 배정 수 합계(" + req.totalQuota() + ")가 선택한 농지 수("
                    + req.getLandNos().size() + ")와 다릅니다.");
            return "redirect:/leader/task-assignment";
        }

        if (leader != null && myTeam != null) {
            // 1. 상위 배정 묶음(DispatchBatches) 생성
            DispatchBatches batch = DispatchBatches.builder()
                    .team(myTeam)
                    .leader(leader)
                    .batchTitle("공동방제 (" + req.getLandNos().size() + "필지)")
                    .assignedPilotCount(req.getQuotas().size())
                    .build();
            dispatchBatchRepository.save(batch);

            // 2. 조종사별 할당량(quotas)에 맞춰 조종사 큐 생성
            List<Long> pilotQueue = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : req.getQuotas().entrySet()) {
                int count = (entry.getValue() != null) ? entry.getValue() : 0;
                for (int i = 0; i < count; i++) {
                    pilotQueue.add(entry.getKey());
                }
            }

						long pilotCount = req.getQuotas().values().stream().filter(v -> v != null && v > 0).count();
            double weight = (pilotCount > 0) ? (1.0 / pilotCount) : 1.0;

            // 3. 선택된 농지에 순서대로 조종사 매핑하여 WorkAssignments 저장
					if (req.getLandNos().size() == 1) {
					/* =========================================================
					* [케이스 A] 농지 1개를 여러 명에게 분할 배정 (N분할)
					* -> 조종사 큐(pilotQueue) 수만큼 WorkAssignments를 여러 개 생성!
					* ========================================================= */
					Long landNo = req.getLandNos().get(0);
					Lands land = landRepository.findById(landNo).orElse(null);

					if (land != null && "READY".equals(land.getStatus())) {
							double totalPieces = pilotQueue.size(); // 총 조각 수 (예: 1:2면 3조각)

							// 조종사별로 담당하는 지분만큼 WorkAssignments 생성
							for (Map.Entry<Long, Integer> entry : req.getQuotas().entrySet()) {
									int quota = (entry.getValue() != null) ? entry.getValue() : 0;
									if (quota > 0) {
											Users pilot = userRepository.findById(entry.getKey()).orElse(leader);
											double myRatio = quota / totalPieces; // 1:2 분할 시 각각 1/3, 2/3

											WorkAssignments assignment = WorkAssignments.builder()
															.dispatchBatch(batch)
															.land(land)
															.pilot(pilot)
															.weightRatio(myRatio) // 조종사 개인 지분율 저장!
															.status("ASSIGNED")
															.build();
											workAssignmentRepository.save(assignment);
									}
							}
							land.updateStatus("ASSIGNED");
							landRepository.save(land);
					}
			} else {
					/* =========================================================
					* [케이스 B] 여러 농지를 여러 명에게 배정 (기존 1인 1필지 로직)
					* ========================================================= */
					for (int i = 0; i < req.getLandNos().size(); i++) {
							Long landNo = req.getLandNos().get(i);
							Lands land = landRepository.findById(landNo).orElse(null);

							if (land != null && "READY".equals(land.getStatus())) {
									Long pilotNo = (i < pilotQueue.size()) ? pilotQueue.get(i) : leader.getNo();
									Users pilot = userRepository.findById(pilotNo).orElse(leader);

									WorkAssignments assignment = WorkAssignments.builder()
													.dispatchBatch(batch)
													.land(land)
													.pilot(pilot)
													.weightRatio(1.0)
													.status("ASSIGNED")
													.build();
									workAssignmentRepository.save(assignment);

									land.updateStatus("ASSIGNED");
									landRepository.save(land);
							}
					}
			}
            redirectAttributes.addFlashAttribute("message",
                    req.getLandNos().size() + "필지를 " + pilotCount + "명의 조종사에게 배정했습니다.");

        }
        return "redirect:/leader/task-assignment?tab=status";
    }

    /** 조각별 배정 한 건 (프론트의 pieceAssignmentsJson 한 칸) */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class PieceAssignment {
        private Integer sliceIndex;   // 구역 번호 (서→동, 0부터)
        private Long pilotNo;         // 담당 조종사 번호
        private Double areaSize;      // 조각 면적(㎡)
        private Object geometry;      // 조각 다각형(GeoJSON) - 저장은 백엔드 작업 후
    }

    /** 프론트에서 온 JSON 글자 → 조각 목록 (실패하면 빈 목록) */
    private List<PieceAssignment> parsePieces(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            PieceAssignment[] arr = objectMapper.readValue(json, PieceAssignment[].class);
            return Arrays.stream(arr)
                    .filter(p -> p != null && p.getPilotNo() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("조각 배정 JSON 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /** 지도에서 직접 그린 영역을 농지(READY)로 저장. 실패하면 null */
    private Lands createDrawnLand(AssignRequest req) {
        String address = (req.getDrawnAddress() == null || req.getDrawnAddress().isBlank())
                ? "직접 그린 영역" : req.getDrawnAddress().trim();
        try {
            return landRepository.save(Lands.builder()
                    .address(address)
                    .latitude(req.getDrawnLat() != null ? req.getDrawnLat() : 36.3504)
                    .longitude(req.getDrawnLng() != null ? req.getDrawnLng() : 127.3845)
                    .areaSize(req.getDrawnArea() != null ? req.getDrawnArea() : 1000.0)
                    .polygonCoordinates(req.getDrawnPolygon())
                    .status("READY")
                    .build());
        } catch (Exception e) {
            // TODO(백엔드): lands.region_id 가 NOT NULL 이라 행정구역 연결 전에는 여기서 실패함
            log.warn("직접 그린 영역 농지 저장 실패: {}", e.getMessage());
            return null;
        }
    }

    /** 농지 1건(또는 직접 그린 영역) → 구역별 조종사에게 배정 (조종사별 면적 비율을 weightRatio 로 저장) */
    private String assignByPieces(AssignRequest req, List<PieceAssignment> pieceList, Users leader, Teams myTeam,
                                  RedirectAttributes redirectAttributes) {
        Lands land;
        if (req.getDrawnPolygon() != null && !req.getDrawnPolygon().isBlank()) {
            land = createDrawnLand(req);
            if (land == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "직접 그린 영역을 농지로 저장하지 못했습니다. (행정구역 연결 작업이 필요합니다)");
                return "redirect:/leader/task-assignment";
            }
        } else {
            if (req.getLandNos() == null || req.getLandNos().size() != 1) {
                redirectAttributes.addFlashAttribute("errorMessage", "구역별 배정은 농지를 1건만 선택해 주세요.");
                return "redirect:/leader/task-assignment";
            }
            land = landRepository.findById(req.getLandNos().get(0)).orElse(null);
        }

        if (land == null || !"READY".equals(land.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "대기 상태의 농지만 배정할 수 있습니다.");
            return "redirect:/leader/task-assignment";
        }

        // 조종사별 담당 면적 합산 (한 명이 여러 구역을 맡아도 한 줄로)
        Map<Long, Double> areaByPilot = new LinkedHashMap<>();
        for (PieceAssignment p : pieceList) {
            areaByPilot.merge(p.getPilotNo(), p.getAreaSize() != null ? p.getAreaSize() : 0.0, Double::sum);
        }
        areaByPilot.keySet().removeIf(no -> !userRepository.existsById(no));
        if (areaByPilot.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "담당 조종사를 찾을 수 없습니다.");
            return "redirect:/leader/task-assignment";
        }
        double total = areaByPilot.values().stream().mapToDouble(Double::doubleValue).sum();

        DispatchBatches batch = DispatchBatches.builder()
                .team(myTeam)
                .leader(leader)
                .batchTitle("영역 분할 방제 (" + pieceList.size() + "구역)")
                .assignedPilotCount(areaByPilot.size())
                .build();
        dispatchBatchRepository.save(batch);

        for (Map.Entry<Long, Double> e : areaByPilot.entrySet()) {
            Users pilot = userRepository.findById(e.getKey()).orElse(null);
            if (pilot == null) continue;

            // TODO(백엔드): 이 조종사가 맡은 구역 다각형(pieceList 중 pilotNo 가 같은 것의 geometry)을
            //               WorkAssignments 에 저장하면 조종사 화면으로 구역 위치를 전달할 수 있음
            workAssignmentRepository.save(WorkAssignments.builder()
                    .dispatchBatch(batch)
                    .land(land)
                    .pilot(pilot)
                    .weightRatio(total > 0 ? e.getValue() / total : 1.0 / areaByPilot.size())
                    .status("ASSIGNED")
                    .build());
        }

        land.updateStatus("ASSIGNED");
        landRepository.save(land);

        redirectAttributes.addFlashAttribute("message",
                pieceList.size() + "개 구역을 " + areaByPilot.size() + "명의 조종사에게 배정했습니다.");
        return "redirect:/leader/task-assignment?tab=status";
    }

    /**
     * 작업 배정 취소 (실제 DB 삭제 및 READY 복구)
     */
    @PostMapping("/task-assignment/cancel")
    public String cancelAssignment(@RequestParam("landNo") Long landNo,
                                   RedirectAttributes redirectAttributes) {
        log.info("배정 취소 요청: landNo={}", landNo);

        Lands land = landRepository.findById(landNo).orElse(null);
        if (land == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 농지입니다.");
            return "redirect:/leader/task-assignment?tab=status";
        }

        // 👈 대기(ASSIGNED) 상태만 취소 허용
        if (!"ASSIGNED".equals(land.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "대기 중인 작업만 취소할 수 있습니다.");
            return "redirect:/leader/task-assignment?tab=status";
        }

        // 👈 findAll() 대신 해당 농지 것만 깔끔하게 조회 후 삭제 (또는 기존 방식도 당장은 OK!)
        List<WorkAssignments> assignments = workAssignmentRepository.findAll().stream()
                .filter(w -> w.getLand() != null && landNo.equals(w.getLand().getNo()))
                .toList();
        workAssignmentRepository.deleteAll(assignments);

        land.updateStatus("READY");
        landRepository.save(land);
        
        redirectAttributes.addFlashAttribute("message", landNo + "번 농지 배정이 취소되었습니다.");
        return "redirect:/leader/task-assignment?tab=status";
    }

    /* =========================================================
       방제단 관리
       ========================================================= */
    @Transactional(readOnly = true)
    @GetMapping("/team-management")
    public String getTeamManagement(
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "status",  defaultValue = "") String status,
            @RequestParam(value = "skill",   defaultValue = "") String skill,
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        if (customUser == null) return "redirect:/login";

        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;
        final String inviteCode = (myTeam != null && myTeam.getInviteCode() != null) ? myTeam.getInviteCode() : "";

        // 조종사별 작업 목록 (상태/현재 작업/금일 완료 계산용)
        Map<Long, List<WorkAssignments>> worksByPilot = workAssignmentRepository.findAll().stream()
                .filter(w -> w.getPilot() != null)
                .collect(Collectors.groupingBy(w -> w.getPilot().getNo()));

        // DB에서 소속 조종사 목록 조회
        List<TeamMemberDto> allMembers = new ArrayList<>();
        if (!inviteCode.isEmpty()) {
            allMembers = userRepository.findAll().stream()
                    .filter(u -> u.getTeam() != null
                              && inviteCode.equals(u.getTeam().getInviteCode())
                              && !"LEADER".equals(u.getRole()))
                    .map(u -> TeamMemberDto.from(u, worksByPilot.getOrDefault(u.getNo(), List.of())))
                    .toList();
        }

        String kw = keyword.trim();
        List<TeamMemberDto> members = allMembers.stream()
                .filter(m -> kw.isEmpty()
                          || (m.getName() != null && m.getName().contains(kw))
                          || (m.getPhone() != null && m.getPhone().contains(kw))
                          || (m.getUsername() != null && m.getUsername().contains(kw)))
                .filter(m -> status.isEmpty() || status.equals(m.getStatus()))
                .filter(m -> skill.isEmpty()  || skill.equals(m.getSkill()))
                .toList();

        long onlineCount  = allMembers.stream().filter(m -> "online".equals(m.getStatus())).count();
        long workingCount = allMembers.stream().filter(m -> "working".equals(m.getStatus())).count();
        int completedSum  = allMembers.stream().mapToInt(TeamMemberDto::getCompleted).sum();

        List<TeamMemberDto> onlineMembers = allMembers.stream()
                .filter(m -> "online".equals(m.getStatus()))
                .toList();

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
        log.info("미완료 작업 재배정: {} → {}", no, targetNo);

        Users targetPilot = userRepository.findById(targetNo).orElse(null);
        if (targetPilot != null) {
            List<WorkAssignments> tasks = workAssignmentRepository.findAll().stream()
                    .filter(w -> w.getPilot() != null && no.equals(w.getPilot().getNo()) && !"COMPLETED".equals(w.getStatus()))
                    .toList();
            for (WorkAssignments w : tasks) {
                w.setPilot(targetPilot);
                workAssignmentRepository.save(w);
            }
            redirectAttributes.addFlashAttribute("message", "미완료 작업이 " + targetPilot.getName() + " 조종사에게 재배정되었습니다.");
        }

        return "redirect:/leader/team-management";
    }

    /* =========================================================
       농지 관리
       ========================================================= */
    @GetMapping("/farmlist")
    public String getFarmList(
            @RequestParam(value = "dong",    defaultValue = "") String dong,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        if (customUser == null) return "redirect:/login";

        // 1. 로그인 단장의 방제단(팀) 정보 가져오기
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;

        List<Lands> rawLands = (myTeam != null) 
                ? landRepository.findByTeam(myTeam) 
                : List.of();
                
        List<LandDto> allLands = rawLands.stream().map(LandDto::from).toList();

        String kw = keyword.trim();
        List<LandDto> lands = allLands.stream()
                .filter(l -> dong.isEmpty() || (l.getDong() != null && l.getDong().equals(dong)))
                .filter(l -> kw.isEmpty() || (l.getAddress() != null && l.getAddress().contains(kw)))
                .toList();

        List<String> dongs = allLands.stream()
                .map(LandDto::getDong)
                .filter(d -> d != null && !d.isBlank())
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
                             @AuthenticationPrincipal CustomUser customUser, // 👈 단장 로그인 세션 주입
                             RedirectAttributes redirectAttributes) {
        log.info("신규 농지 등록 요청: {}", form);

        if (customUser == null) return "redirect:/login";

        // 1. 단장의 방제단(팀) 정보 조회
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;

        if (myTeam == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "소속된 방제단 정보가 없습니다.");
            return "redirect:/leader/farmlist";
        }

        // 2. ⭐ 우리 팀에 해당 주소로 아직 완료되지 않은 농지가 있는지 중복 검증!
        String targetAddress = form.getAddress().trim();
        if (landRepository.existsByTeamAndAddressAndStatusNot(myTeam, targetAddress, "COMPLETED")) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "해당 농지(" + targetAddress + ")는 현재 방제 대기 또는 진행 중인 작업이 있습니다.");
            return "redirect:/leader/farmlist";
        }

        // 3. 검증 통과 시 방제단(team)을 포함하여 신규 등록
        Lands land = Lands.builder()
                .address(targetAddress)
                .team(myTeam) // 👈 팀 소속 확실하게 저장!
                .latitude(form.getLatitude() != null ? form.getLatitude() : 36.3504)
                .longitude(form.getLongitude() != null ? form.getLongitude() : 127.3845)
                .areaSize(form.getSafeAreaSize())
                .status("READY")
                .polygonCoordinates(form.getPolygonCoordinates())
                .build();

        landRepository.save(land);
        log.info("농지 등록 완료: no={}, address={}, team={}", land.getNo(), targetAddress, myTeam.getTeamName());

        redirectAttributes.addFlashAttribute("message", targetAddress + " 농지가 성공적으로 등록되었습니다.");
        return "redirect:/leader/farmlist";
    }

    @PostMapping("/farmlist/delete")
    public String deleteLand(@RequestParam("landNo") Long landNo,
                             RedirectAttributes redirectAttributes) {
        log.info("농지 삭제: landNo={}", landNo);

        Lands land = landRepository.findById(landNo).orElse(null);
        if (land != null && "READY".equals(land.getStatus())) {
            landRepository.delete(land);
            redirectAttributes.addFlashAttribute("message", landNo + "번 농지가 삭제되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("message", "배정 중이거나 작업 중인 농지는 삭제할 수 없습니다.");
        }

        return "redirect:/leader/farmlist";
    }

    @GetMapping("/history")
    public String getHistory() {
        return "redirect:/leader/task-assignment?tab=history";
    }

    /* =========================================================
       작업 이력 검색 (실제 DB WorkAssignments 조회)
       ========================================================= */
    private List<PilotTaskDto> filterHistory(LocalDate from, LocalDate to, String region, String pilot) {
        // DB에서 완료(COMPLETED)된 작업만 가져와 PilotTaskDto로 매핑
        List<WorkAssignments> completed = workAssignmentRepository.findAll().stream()
                .filter(w -> "COMPLETED".equals(w.getStatus()))
                .toList();

        List<PilotTaskDto> dtos = completed.stream().map(w -> {
            // 읍/면/동 행정구역 이름 추출
            String regionName = (w.getLand() != null && w.getLand().getRegion() != null)
                    ? w.getLand().getRegion().getEupmyeondong()
                    : "-";

            return PilotTaskDto.builder()
                    .no(w.getNo())
                    .address(w.getLand() != null ? w.getLand().getAddress() : "-")
                    .region(regionName)
                    .areaSize(w.getLand() != null ? w.getLand().getAreaSize() : 0.0)
                    .status(w.getStatus())
                    .pilotName(w.getPilot() != null ? w.getPilot().getName() : "-")
                    .assignedAt(w.getCreatedAt())
                    .completedAt(w.getCompletedAt() != null ? w.getCompletedAt() : w.getCreatedAt())
                    .chemicalName("기본 살충제")
                    .usedAmount("0.0")
                    .build();
        }).toList();

        String[] words = region.trim().isEmpty() ? new String[0] : region.trim().split("\\s+");
        return dtos.stream()
                .filter(h -> from == null || (h.getCompletedAt() != null && !h.getCompletedAt().toLocalDate().isBefore(from)))
                .filter(h -> to == null   || (h.getCompletedAt() != null && !h.getCompletedAt().toLocalDate().isAfter(to)))
                .filter(h -> Arrays.stream(words)
                        .allMatch(w -> ((h.getRegion() != null ? h.getRegion() : "") + " " + (h.getAddress() != null ? h.getAddress() : "")).contains(w)))
                .filter(h -> pilot.isEmpty() || pilot.equals(h.getPilotName()))
                .sorted(Comparator.comparing(PilotTaskDto::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

        private String csv(Object value) {
                String s = value == null ? "" : String.valueOf(value);
                return "\"" + s.replace("\"", "\"\"") + "\"";
        }

}