package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.tasks.Chemicals;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.tasks.AssetCreateRequest;
import com.aloha.project.dto.tasks.AssetDto;
import com.aloha.project.domain.common.Pagination;
import com.aloha.project.repository.tasks.DroneRepository;
import com.aloha.project.repository.tasks.ChemicalRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.repository.tasks.WorkAssignmentResourceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



/**
 * 방제단장 - 드론·약제 관리
 * 예) /leader/assetlist?tab=drone&owner=김민수&id=DR-001
 */
@Slf4j
@Controller
@RequestMapping("/leader")
@RequiredArgsConstructor
public class LeaderAssetController {

    private final DroneRepository droneRepository;
    private final ChemicalRepository chemicalRepository;
    private final UserRepository userRepository;
    private final WorkAssignmentResourceRepository workAssignmentResourceRepository;

    @Transactional(readOnly = true)
    @GetMapping("/assetlist")
    public String getAssetList(
            @RequestParam(value = "tab",     defaultValue = "all") String tab,
            @RequestParam(value = "type",    defaultValue = "") String type,
            @RequestParam(value = "owner",   defaultValue = "") String owner,
            @RequestParam(value = "status",  defaultValue = "") String status,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "page",    defaultValue = "1") int page,
            @RequestParam(value = "size",    defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUser customUser, // 👈 로그인 단장 주입
            Model model) {

        if (customUser == null || customUser.getUser() == null) {
            return "redirect:/login";
        }

        // 로그인 사용자 조회
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);

        // 단장 권한 확인
        if (leader == null || !"LEADER".equals(leader.getRole())) {
            return "redirect:/";
        }

        // 소속 방제단 확인
        Teams myTeam = leader.getTeam();

        if (myTeam == null) {
            return "redirect:/";
        }

        final String inviteCode = myTeam.getInviteCode() != null
                ? myTeam.getInviteCode()
                : "";

        // [DB 연동 2] 실제 우리 방제단의 드론·약제 목록을 AssetDto로 변환
        List<AssetDto> all = new ArrayList<>();
        
        if (!inviteCode.isEmpty()) {
        
            // 1. 우리 방제단 소속 드론 조회
            all.addAll(droneRepository.findAll().stream()
                    .filter(d -> d.getUser() != null
                            && d.getUser().getTeam() != null
                            && inviteCode.equals(d.getUser().getTeam().getInviteCode()))
                    .map(d -> {
                        AssetDto dto = AssetDto.from(d);
                        if (workAssignmentResourceRepository
                                .existsByDrone_NoAndWorkAssignment_Status(d.getNo(), "IN_PROGRESS")) {
                            dto.setStatus("operating");
                        }
                        return dto;
                    })
                    .toList());
        
            // 2. 우리 방제단 소속 약제 조회
            all.addAll(chemicalRepository.findByTeam_No(myTeam.getNo()).stream()
                    .map(AssetDto::from)
                    .toList());
        }

        // ========================================================
        // 👇 프론트엔드 팀원이 작성한 탭 필터링 & 통계 로직 (100% 보존!)
        // ========================================================

        // 0) 탭에 맞는 검색값만 사용
        //    - 자산 유형: 전체 탭에서만
        //    - 상태: 그 탭의 상태 목록에 있을 때만
        String typeFilter = "all".equals(tab) ? type : "";
        Map<String, String> statusOptions = statusOptionsFor(tab);
        String statusFilter = statusOptions.containsKey(status) ? status : "";

        // 1) 탭 + 검색 조건으로 거르기
        String kw = keyword.trim();
        List<AssetDto> assets = all.stream()
                .filter(a -> switch (tab) {
                    case "drone"    -> a.isDrone();
                    case "chemical" -> !a.isDrone();
                    default         -> true;
                })
                .filter(a -> typeFilter.isEmpty()   || a.getType().equals(typeFilter))
                .filter(a -> owner.isEmpty()        || owner.equals(a.getOwner()))
                .filter(a -> statusFilter.isEmpty() || a.getStatus().equals(statusFilter))
                .filter(a -> kw.isEmpty()
                          || (a.getId() + " " + a.getName() + " " + a.getSubtitle() + " " + a.getOwner()).contains(kw))
                .toList();


        // ========================================================
        // 페이지네이션
        // ========================================================

       
         size = (size < 1 || size > 100) ? 10 : size;

        Pagination pagination = new Pagination(
              Math.max(page, 1),
              size,
              10,
              assets.size()
               );

        // 마지막 페이지보다 큰 페이지 요청 방지
        if (pagination.getPage() > pagination.getLast()) {
        pagination = new Pagination(pagination.getLast(), size, 10, assets.size());
        }

        int from = (int) Math.min(pagination.getIndex(), assets.size());
        int to = (int) Math.min(from + size, assets.size());

        List<AssetDto> pagedAssets = assets.subList(from, to);



        // 2) 요약 카드 (전체 기준)
        long droneCount       = all.stream().filter(AssetDto::isDrone).count();
        long chemicalCount    = all.size() - droneCount;
        long maintenanceCount = all.stream().filter(AssetDto::isMaintenanceNeeded).count();


        // 3) 셀렉트 옵션
        // 같은 방제단 소속 조종사 목록
        List<Users> owners = (myTeam == null)
                ? new ArrayList<>()
                : userRepository.findByTeam(myTeam).stream()
                        .filter(user -> "PILOT".equals(user.getRole()))
                        .toList();

        String tabTitle = switch (tab) {
            case "drone"    -> "드론 기체";
            case "chemical" -> "농업용 약제";
            default         -> "전체 자산";
        };

        model.addAttribute("tab", tab);
        model.addAttribute("tabTitle", tabTitle);
        model.addAttribute("assets", pagedAssets);
        model.addAttribute("pagination", pagination);
        model.addAttribute("droneCount", droneCount);
        model.addAttribute("chemicalCount", chemicalCount);
        model.addAttribute("maintenanceCount", maintenanceCount);
        model.addAttribute("owners", owners);
        model.addAttribute("statusOptions", statusOptions);                 // 검색용 (탭별)
        model.addAttribute("allStatusOptions", statusOptionsFor("all"));     // 자산 등록 팝업용 (전부)
        // 검색값 유지
        model.addAttribute("type", typeFilter);
        model.addAttribute("owner", owner);
        model.addAttribute("status", statusFilter);
        model.addAttribute("keyword", keyword);

        return "page/leader/assetlist";
    }

    
    /**
     * 자산 등록 (단장 권한)
     * - 같은 방제단의 PILOT에게만 등록 가능
     */
    @PostMapping("/assetlist")
    public String createAsset(
            @ModelAttribute AssetCreateRequest form,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes redirectAttributes) {

        log.info("자산 등록 요청: {}", form);

        if (customUser == null || customUser.getUser() == null) {
            return "redirect:/login";
        }

        // 로그인 사용자 조회
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);

        // 단장 권한 확인
        if (leader == null || !"LEADER".equals(leader.getRole())) {
            redirectAttributes.addFlashAttribute("error", "단장만 자산을 등록할 수 있습니다.");
            return "redirect:/leader/assetlist";
        }

        // 소속 방제단 확인
        if (leader.getTeam() == null) {
            redirectAttributes.addFlashAttribute("error", "소속 방제단 정보를 찾을 수 없습니다.");
            return "redirect:/leader/assetlist";
        }

        // 담당 조종사 입력값 확인
        if (form.getOwner() == null || form.getOwner().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "담당 조종사를 선택해 주세요.");
            return "redirect:/leader/assetlist";
        }

        Long pilotNo;

        try {
            pilotNo = Long.parseLong(form.getOwner());
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "올바른 담당 조종사를 선택해 주세요.");
            return "redirect:/leader/assetlist";
        }

        // 선택한 조종사 조회
        Users pilot = userRepository.findById(pilotNo).orElse(null);

        // 같은 방제단 소속 PILOT인지 확인
        if (pilot == null
                || !"PILOT".equals(pilot.getRole())
                || pilot.getTeam() == null
                || !Objects.equals(
                        leader.getTeam().getNo(),
                        pilot.getTeam().getNo())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "같은 방제단에 소속된 조종사만 담당자로 지정할 수 있습니다."
            );
            return "redirect:/leader/assetlist";
        }

        // 자산 이름 확인
        if (form.getName() == null || form.getName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "자산 이름을 입력해 주세요.");
            return "redirect:/leader/assetlist";
        }

        // 자산 유형에 따라 저장
        if ("drone".equals(form.getType())) {

            Drones drone = new Drones();
            drone.setUser(pilot);
            drone.setModelName(form.getName().trim());

            drone.setStatus(AssetDto.toDbStatus(
                    form.getStatus() != null && !form.getStatus().isBlank()
                            ? form.getStatus()
                            : "normal"
            ));

            droneRepository.save(drone);

        } else if ("chemical".equals(form.getType())) {

            Chemicals chemical = new Chemicals();
            chemical.setTeam(leader.getTeam());
            chemical.setUser(pilot);
            chemical.setChemicalName(form.getName().trim());
            chemical.setCategory(form.getSubtitle());

            chemical.setStatus(
                    form.getStatus() != null && !form.getStatus().isBlank()
                            ? form.getStatus()
                            : "normal"
            );

            chemicalRepository.save(chemical);

        } else {
            redirectAttributes.addFlashAttribute("error", "올바르지 않은 자산 유형입니다.");
            return "redirect:/leader/assetlist";
        }

        redirectAttributes.addFlashAttribute("assetRegistered", true);

        return "redirect:/leader/assetlist";
    }

    /**
     * 자산 상태 변경 (단장 권한)
     * - 같은 방제단의 드론·약제만 변경 가능
     */
    @Transactional
    @PostMapping("/assetlist/{id}/status")
    public String changeStatus(
            @PathVariable("id") String id,
            @RequestParam("status") String status,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes redirectAttributes) {

        log.info("자산 상태 변경 요청: {} → {}", id, status);

        if (customUser == null || customUser.getUser() == null) {
            return "redirect:/login";
        }

        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);

        // 단장 권한 확인
        if (leader == null
                || !"LEADER".equals(leader.getRole())
                || leader.getTeam() == null) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "단장 권한이 없거나 소속 방제단 정보를 찾을 수 없습니다."
            );
            return "redirect:/leader/assetlist";
        }

        try {

            // 1. 드론 상태 변경
            if (id.startsWith("drone-")) {

                Long droneNo = Long.parseLong(id.substring("drone-".length()));

                Drones drone = droneRepository.findById(droneNo).orElse(null);

                // 드론 존재 여부 및 같은 방제단 확인
                if (drone == null
                        || drone.getUser() == null
                        || drone.getUser().getTeam() == null
                        || !Objects.equals(
                                leader.getTeam().getNo(),
                                drone.getUser().getTeam().getNo())) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "드론 정보를 찾을 수 없거나 변경 권한이 없습니다."
                    );

                } else if (!List.of(
                        "normal",
                        "maintenance",
                        "in-maintenance",
                        "fault"
                ).contains(status)) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "올바르지 않은 드론 상태입니다."
                    );

                } else if (workAssignmentResourceRepository
                        .existsByDrone_NoAndWorkAssignment_Status(droneNo, "IN_PROGRESS")) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "작업 중인 드론은 모든 상태 변경이 작업 종료 후 가능합니다."
                    );

                } else {

                    drone.setStatus(AssetDto.toDbStatus(status));
                    droneRepository.save(drone);

                    redirectAttributes.addFlashAttribute(
                            "message",
                            drone.getModelName() + " 상태가 '"
                                    + AssetDto.STATUS_LABELS.getOrDefault(status, status)
                                    + "'(으)로 변경되었습니다."
                    );
                }

            // 2. 약제 상태 변경
            } else if (id.startsWith("chemical-")) {

                Long chemicalNo = Long.parseLong(id.substring("chemical-".length()));

                Chemicals chemical = chemicalRepository.findById(chemicalNo).orElse(null);

                // 약제 존재 여부 및 같은 방제단 확인
                if (chemical == null
                        || chemical.getTeam() == null
                        || !Objects.equals(
                                leader.getTeam().getNo(),
                                chemical.getTeam().getNo())) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "약제 정보를 찾을 수 없거나 변경 권한이 없습니다."
                    );

                } else if (!List.of("normal", "low").contains(status)) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "올바르지 않은 약제 상태입니다."
                    );

                } else {

                    chemical.setStatus(status);
                    chemicalRepository.save(chemical);

                    redirectAttributes.addFlashAttribute(
                            "message",
                            chemical.getChemicalName() + " 상태가 '"
                                    + AssetDto.STATUS_LABELS.getOrDefault(status, status)
                                    + "'(으)로 변경되었습니다."
                    );
                }

            } else {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "올바르지 않은 자산 ID입니다."
                );
            }

        } catch (NumberFormatException e) {

            log.warn("잘못된 자산 ID: {}", id);

            redirectAttributes.addFlashAttribute(
                    "error",
                    "잘못된 자산 번호입니다."
            );
        }

        return "redirect:/leader/assetlist";
    }

    /**
     * 자산 삭제 (단장 권한)
     * - 같은 방제단의 자산만 삭제 가능
     * - 작업에 사용된 자산은 삭제 불가
     */
    @Transactional
    @PostMapping("/assetlist/{id}/delete")
    public String deleteAsset(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes redirectAttributes) {

        log.info("자산 삭제 요청: {}", id);

        if (customUser == null || customUser.getUser() == null) {
            return "redirect:/login";
        }

        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);

        // 단장 권한 및 소속 방제단 확인
        if (leader == null
                || !"LEADER".equals(leader.getRole())
                || leader.getTeam() == null) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "삭제 권한이 없거나 소속 방제단 정보를 찾을 수 없습니다."
            );

            return "redirect:/leader/assetlist";
        }

        try {

            // 1. 드론 삭제
            if (id.startsWith("drone-")) {

                Long droneNo = Long.parseLong(id.substring("drone-".length()));

                Drones drone = droneRepository.findById(droneNo).orElse(null);

                // 드론 존재 여부 및 같은 방제단 확인
                if (drone == null
                        || drone.getUser() == null
                        || drone.getUser().getTeam() == null
                        || !Objects.equals(
                                leader.getTeam().getNo(),
                                drone.getUser().getTeam().getNo())) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "드론 정보를 찾을 수 없거나 삭제 권한이 없습니다."
                    );

                // 작업 배정 자원으로 사용 중인지 확인
                } else if (workAssignmentResourceRepository.existsByDrone_No(droneNo)) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "작업에 사용된 드론은 삭제할 수 없습니다."
                    );

                } else {

                    String droneName = drone.getModelName();

                    droneRepository.delete(drone);

                    redirectAttributes.addFlashAttribute(
                            "message",
                            droneName + " 드론이 삭제되었습니다."
                    );
                }

            // 2. 약제 삭제
            } else if (id.startsWith("chemical-")) {

                Long chemicalNo = Long.parseLong(id.substring("chemical-".length()));

                Chemicals chemical = chemicalRepository.findById(chemicalNo).orElse(null);

                // 약제 존재 여부 및 같은 방제단 확인
                if (chemical == null
                        || chemical.getTeam() == null
                        || !Objects.equals(
                                leader.getTeam().getNo(),
                                chemical.getTeam().getNo())) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "약제 정보를 찾을 수 없거나 삭제 권한이 없습니다."
                    );

                // 작업 배정 자원으로 사용 중인지 확인
                } else if (workAssignmentResourceRepository.existsByChemical_No(chemicalNo)) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "작업에 사용된 약제는 삭제할 수 없습니다."
                    );

                } else {

                    String chemicalName = chemical.getChemicalName();

                    chemicalRepository.delete(chemical);

                    redirectAttributes.addFlashAttribute(
                            "message",
                            chemicalName + " 약제가 삭제되었습니다."
                    );
                }

            } else {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "올바르지 않은 자산 ID입니다."
                );
            }

        } catch (NumberFormatException e) {

            log.warn("잘못된 자산 ID: {}", id);

            redirectAttributes.addFlashAttribute(
                    "error",
                    "잘못된 자산 번호입니다."
            );
        }

        return "redirect:/leader/assetlist";
    }

    /* =========================================================
       탭별 상태 검색 목록 (AssetDto.STATUS_LABELS 에서 골라 씀, DTO 는 수정 안 함)
       ========================================================= */
    private Map<String, String> statusOptionsFor(String tab) {
    List<String> keys = switch (tab) {
        case "drone" ->
            List.of("normal", "operating", "maintenance",
                    "in-maintenance", "fault");
        case "chemical" -> List.of("normal", "low");
        default -> List.of("normal", "operating", "maintenance",
                           "in-maintenance", "fault", "low");
    };

    Map<String, String> options = new LinkedHashMap<>();
    keys.forEach(k -> options.put(k, AssetDto.STATUS_LABELS.get(k)));
    return options;
}
}