
package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
import com.aloha.project.repository.tasks.WorkAssignmentResourceRepository;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.AssetDto;
import com.aloha.project.repository.tasks.DroneRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.dto.AssetCreateRequest;
import com.aloha.project.domain.tasks.Chemicals;
import com.aloha.project.repository.tasks.ChemicalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;




/**
 * 조종사 - 드론·자산 (본인 및 같은 방제단 드론 조회, 보기 전용)
 * 예) /pilot/assetlist?tab=drone&id=DR-001
 */
@Slf4j
@Controller
@RequestMapping("/pilot")
@RequiredArgsConstructor
public class PilotAssetController {

    private final UserRepository userRepository;
    private final DroneRepository droneRepository;
    private final ChemicalRepository chemicalRepository;
    private final WorkAssignmentResourceRepository workAssignmentResourceRepository;

    @Transactional(readOnly = true)
    @GetMapping("/assetlist")
    public String getAssetList(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            @RequestParam(value = "type", defaultValue = "") String type,
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "assetType", required = false) String assetType,
            Model model) {

        // 1. 로그인 조종사 확인
        if (customUser == null || customUser.getUser() == null) {
            throw new IllegalStateException("조종사 로그인이 필요합니다.");
        }

        Users pilot = userRepository.findById(customUser.getUser().getNo())
                .orElseThrow(() ->
                        new IllegalArgumentException("로그인 사용자를 찾을 수 없습니다."));

        if (!"PILOT".equals(pilot.getRole())) {
            throw new IllegalStateException("조종사 계정만 접근할 수 있습니다.");
        }

        // 같은 방제단 소속 조종사 목록
        List<Users> owners = userRepository.findAll().stream()
        .filter(u -> "PILOT".equals(u.getRole()))
        .filter(u -> u.getTeam() != null
                && pilot.getTeam() != null
                && u.getTeam().getNo().equals(pilot.getTeam().getNo()))
        .toList();

       
        // 2. 본인 및 같은 방제단의 드론 조회
        Map<Long, Drones> droneMap = new LinkedHashMap<>();

        // 본인 소유 드론
        droneRepository.findByUser_No(pilot.getNo())
                .forEach(drone -> droneMap.put(drone.getNo(), drone));

        // 같은 방제단 전체 드론
        if (pilot.getTeam() != null) {
            droneRepository.findByUser_Team_No(pilot.getTeam().getNo())
                    .forEach(drone -> droneMap.put(drone.getNo(), drone));
        }

        // 3. Entity → DTO 변환
        List<AssetDto> all = new ArrayList<>(
        droneMap.values().stream()
                .map(AssetDto::from)
                .toList()
        );

        // 같은 방제단의 약제 조회
        if (pilot.getTeam() != null) {
         List<AssetDto> chemicals = chemicalRepository
            .findByTeam_No(pilot.getTeam().getNo())
            .stream()
            .map(AssetDto::from)
            .toList();

         all.addAll(chemicals);
        }


        // 4. 기존 탭 및 검색 조건
        String typeFilter = "all".equals(tab) ? type : "";
        Map<String, String> statusOptions = statusOptionsFor(tab);
        String statusFilter = statusOptions.containsKey(status)
                ? status : "";

        String kw = keyword.trim();

        List<AssetDto> assets = all.stream()
                .filter(a -> switch (tab) {
                    case "drone" -> a.isDrone();
                    case "chemical" -> !a.isDrone();
                    default -> true;
                })
                .filter(a -> typeFilter.isEmpty()
                        || a.getType().equals(typeFilter))
                .filter(a -> statusFilter.isEmpty()
                        || a.getStatus().equals(statusFilter))
                .filter(a -> kw.isEmpty()
                        || (a.getId() + " " + a.getName() + " "
                                + a.getSubtitle()).contains(kw))
                .toList();

        // 5. 요약 카드
        long droneCount = all.stream()
                .filter(AssetDto::isDrone)
                .count();

        long chemicalCount = all.size() - droneCount;

        long maintenanceCount = all.stream()
                .filter(AssetDto::isMaintenanceNeeded)
                .count();
        
        String tabTitle = switch (tab) {
        case "drone" -> "드론 기체";
        case "chemical" -> "농업용 약제";
        default -> "전체 자산";
        };

       // 6. 상세 조회
        String selectedType = assetType;

       // 현재 탭이 드론 또는 약제라면 탭 정보를 우선 사용
         if ("drone".equals(tab)) {
        selectedType = "drone";
       } else if ("chemical".equals(tab)) {
       selectedType = "chemical";
      }

        // 람다에서 사용할 최종 변수
        final String detailType = selectedType;

        AssetDto selected = (id == null) ? null
        : all.stream()
                .filter(a -> a.getId().equals(id))
                .filter(a -> detailType == null
                        || detailType.isEmpty()
                        || a.getType().equals(detailType))
                .findFirst()
                .orElse(null);
        

        // 7. 기존 화면 Model 변수명 유지
        model.addAttribute("tab", tab);
        model.addAttribute("tabTitle", tabTitle);
        model.addAttribute("assets", assets);
        model.addAttribute("droneCount", droneCount);
        model.addAttribute("chemicalCount", chemicalCount);
        model.addAttribute("maintenanceCount", maintenanceCount);
        model.addAttribute("selected", selected);
        model.addAttribute("statusOptions", statusOptions);
        model.addAttribute("type", typeFilter);
        model.addAttribute("status", statusFilter);
        model.addAttribute("keyword", keyword);
        model.addAttribute("owners", owners);
        model.addAttribute("currentUser", pilot);
        model.addAttribute("allStatusOptions", statusOptionsFor("all"));

        return "page/pilot/assetlist";
    }

      @PostMapping("/assetlist")
      public String createAsset(
        @ModelAttribute AssetCreateRequest form,
        @AuthenticationPrincipal CustomUser customUser,
        RedirectAttributes redirectAttributes) {

      if (customUser == null || customUser.getUser() == null) {
        return "redirect:/login";
    }

      Users pilot = userRepository.findById(customUser.getUser().getNo())
            .orElseThrow(() ->
                    new IllegalArgumentException("로그인 사용자를 찾을 수 없습니다."));

       if (!"PILOT".equals(pilot.getRole())) {
        throw new IllegalStateException("조종사 계정만 등록할 수 있습니다.");
    }

       if ("chemical".equals(form.getType())) {

    // 약제 등록
    if (pilot.getTeam() == null) {
        throw new IllegalStateException("방제단에 소속된 조종사만 약제를 등록할 수 있습니다.");
    }

    Chemicals chemical = Chemicals.builder()
        .team(pilot.getTeam())
        .user(pilot)
        .chemicalName(form.getName())
        .category(form.getSubtitle())
        .status(form.getStatus() != null ? form.getStatus() : "normal")
        .build();

    chemicalRepository.save(chemical);

    } else {

     // 기존 드론 등록
     Drones drone = new Drones();
     drone.setUser(pilot);
     drone.setModelName(form.getName());
     
      drone.setStatus(AssetDto.toDbStatus(
        form.getStatus() != null && !form.getStatus().isBlank()
                ? form.getStatus()
                : "normal"
         ));
 
      droneRepository.save(drone);
        }

       redirectAttributes.addFlashAttribute(
        "assetRegistered", true
        );

        return "redirect:/pilot/assetlist";
        }

        /**
 * 자산 상태 변경 (본인 소유 자산만 가능)
 */
        @PostMapping("/assetlist/{id}/status")
    public String changeStatus(@PathVariable("id") String id,
                            @RequestParam("status") String status,
                            @AuthenticationPrincipal CustomUser customUser,
                            RedirectAttributes redirectAttributes) {
                        
                log.info("자산 상태 변경 요청: {} → {}", id, status); 

                // 1. 로그인 확인
                if (customUser == null || customUser.getUser() == null) {
                return "redirect:/login";
                 }

                // 2. 로그인한 파일럿 조회
                 Users pilot = userRepository.findById(customUser.getUser().getNo())
                  .orElse(null);

                if (pilot == null || !"PILOT".equals(pilot.getRole())) {
                redirectAttributes.addFlashAttribute(
                "error", "파일럿 계정만 자산 상태를 변경할 수 있습니다."
                 );
                  return "redirect:/pilot/assetlist";
                  }

                try {
                   // 1. 드론 상태 변경
            if (id.startsWith("drone-")) {

                Long droneNo = Long.parseLong(id.substring("drone-".length()));

                Drones drone = droneRepository.findById(droneNo).orElse(null);

                if (drone == null || drone.getUser() == null
                  || !pilot.getNo().equals(drone.getUser().getNo())) {

                    redirectAttributes.addFlashAttribute(
                            "error", "드론 정보를 찾을 수 없거나 변경 권한이 없습니다."
                    );

                } else if (!List.of("normal", "operating", "maintenance",
                        "in-maintenance", "fault").contains(status)) {

                    redirectAttributes.addFlashAttribute(
                            "error", "올바르지 않은 드론 상태입니다."
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
               
               // 4. 약제 상태 변경
        } else if (id.startsWith("chemical-")) {

            Long chemicalNo = Long.parseLong(id.substring("chemical-".length()));

            Chemicals chemical = chemicalRepository.findById(chemicalNo).orElse(null);

            // 본인이 담당하는 약제인지 확인
            if (chemical == null || chemical.getUser() == null
                    || !pilot.getNo().equals(chemical.getUser().getNo())) {

                redirectAttributes.addFlashAttribute(
                        "error", "약제 정보를 찾을 수 없거나 변경 권한이 없습니다."
                );

            } else if (!List.of("normal", "low").contains(status)) {

                redirectAttributes.addFlashAttribute(
                        "error", "올바르지 않은 약제 상태입니다."
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
                    "error", "올바르지 않은 자산 ID입니다."
                 );
               }
 
            } catch (NumberFormatException e) {
             log.warn("잘못된 자산 ID: {}", id);

            redirectAttributes.addFlashAttribute(
                "error", "잘못된 자산 번호입니다."
          );
        }

           // 5. 변경 후 기존 자산 상세 화면 유지
            redirectAttributes.addAttribute("id", id);

              return "redirect:/pilot/assetlist";
         }

        /**
 * 파일럿 자산 삭제 (본인 소유 자산만 가능)
 */
@Transactional
@PostMapping("/assetlist/{id}/delete")
public String deleteAsset(
        @PathVariable("id") String id,
        @AuthenticationPrincipal CustomUser customUser,
        RedirectAttributes redirectAttributes) {

    if (customUser == null || customUser.getUser() == null) {
        return "redirect:/login";
    }

    Users pilot = userRepository.findById(customUser.getUser().getNo())
            .orElse(null);

    if (pilot == null || !"PILOT".equals(pilot.getRole())) {
        redirectAttributes.addFlashAttribute("error", "파일럿 계정만 삭제할 수 있습니다.");
        return "redirect:/pilot/assetlist";
    }

    try {
        if (id.startsWith("drone-")) {
            Long no = Long.parseLong(id.substring("drone-".length()));
            Drones drone = droneRepository.findById(no).orElse(null);

            if (drone == null || drone.getUser() == null
                    || !pilot.getNo().equals(drone.getUser().getNo())) {
                redirectAttributes.addFlashAttribute("error", "삭제 권한이 없습니다.");
            } else if (workAssignmentResourceRepository.existsByDrone_No(no)) {
                redirectAttributes.addFlashAttribute("error", "작업 배정에 사용 중인 드론은 삭제할 수 없습니다.");
            } else {
                droneRepository.delete(drone);
                redirectAttributes.addFlashAttribute("message", "드론이 삭제되었습니다.");
            }

        } else if (id.startsWith("chemical-")) {
            Long no = Long.parseLong(id.substring("chemical-".length()));
            Chemicals chemical = chemicalRepository.findById(no).orElse(null);

            if (chemical == null || chemical.getUser() == null
                    || !pilot.getNo().equals(chemical.getUser().getNo())) {
                redirectAttributes.addFlashAttribute("error", "삭제 권한이 없습니다.");
            } else if (workAssignmentResourceRepository.existsByChemical_No(no)) {
                redirectAttributes.addFlashAttribute("error", "작업 배정에 사용 중인 약제는 삭제할 수 없습니다.");
            } else {
                chemicalRepository.delete(chemical);
                redirectAttributes.addFlashAttribute("message", "약제가 삭제되었습니다.");
            }

        } else {
            redirectAttributes.addFlashAttribute("error", "올바르지 않은 자산 ID입니다.");
        }

    } catch (NumberFormatException e) {
        redirectAttributes.addFlashAttribute("error", "잘못된 자산 번호입니다.");
    }

    return "redirect:/pilot/assetlist";
}


    /* =========================================================
       탭별 상태 검색 목록
       ========================================================= */
    private Map<String, String> statusOptionsFor(String tab) {

        List<String> keys = switch (tab) {
            case "drone" ->
                    List.of("normal", "operating", "maintenance",
                            "in-maintenance", "fault");
            case "chemical" ->
                    List.of("normal", "low");
            default -> List.of("normal", "operating", "maintenance","in-maintenance", "fault", "low"
);
        };

        Map<String, String> options = new LinkedHashMap<>();

        keys.forEach(k ->
                options.put(k, AssetDto.STATUS_LABELS.get(k)));

        return options;
    }
}
