
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

import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.AssetDto;
import com.aloha.project.repository.tasks.DroneRepository;
import com.aloha.project.repository.users.UserRepository;

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

    @Transactional(readOnly = true)
    @GetMapping("/assetlist")
    public String getAssetList(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            @RequestParam(value = "type", defaultValue = "") String type,
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "id", required = false) String id,
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
        List<AssetDto> all = droneMap.values().stream()
                .map(AssetDto::from)
                .toList();


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

        // 6. 상세 조회
        AssetDto selected = (id == null) ? null
                : all.stream()
                        .filter(a -> a.getId().equals(id))
                        .findFirst()
                        .orElse(null);

        String tabTitle = switch (tab) {
            case "drone" -> "드론 기체";
            case "chemical" -> "농업용 약제";
            default -> "전체 자산";
        };

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

        return "page/pilot/assetlist";
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
            default ->
                    new ArrayList<>(AssetDto.STATUS_LABELS.keySet());
        };

        Map<String, String> options = new LinkedHashMap<>();
        keys.forEach(k ->
                options.put(k, AssetDto.STATUS_LABELS.get(k)));

        return options;
    }
}
