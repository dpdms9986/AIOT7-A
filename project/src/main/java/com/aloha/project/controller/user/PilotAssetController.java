package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aloha.project.dto.AssetDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 조종사 - 드론·자산 (내 담당 자산만, 보기 전용)
 * 예) /pilot/assetlist?tab=drone&id=DR-001
 */
@Slf4j
@Controller
@RequestMapping("/pilot")
public class PilotAssetController {

    @GetMapping("/assetlist")
    public String getAssetList(
            @RequestParam(value = "tab",     defaultValue = "all") String tab,
            @RequestParam(value = "type",    defaultValue = "") String type,
            @RequestParam(value = "status",  defaultValue = "") String status,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "id",      required = false) String id,
            Model model) {

        // TODO(백엔드): assetService.getMyAssets(로그인 조종사)
        List<AssetDto> all = new ArrayList<>();

        // 0) 탭에 맞는 검색값만 사용
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
                .filter(a -> statusFilter.isEmpty() || a.getStatus().equals(statusFilter))
                .filter(a -> kw.isEmpty()
                          || (a.getId() + " " + a.getName() + " " + a.getSubtitle()).contains(kw))
                .toList();

        // 2) 요약 카드
        long droneCount       = all.stream().filter(AssetDto::isDrone).count();
        long chemicalCount    = all.size() - droneCount;
        long maintenanceCount = all.stream().filter(AssetDto::isMaintenanceNeeded).count();

        // 3) 상세 (주소에 id 가 있을 때만)
        AssetDto selected = (id == null) ? null
                : all.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);

        String tabTitle = switch (tab) {
            case "drone"    -> "드론 기체";
            case "chemical" -> "농업용 약제";
            default         -> "전체 자산";
        };

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
       탭별 상태 검색 목록 (AssetDto.STATUS_LABELS 에서 골라 씀, DTO 는 수정 안 함)
       ========================================================= */
    private Map<String, String> statusOptionsFor(String tab) {
        List<String> keys = switch (tab) {
            case "drone"    -> List.of("normal", "operating", "maintenance", "in-maintenance", "fault");
            case "chemical" -> List.of("normal", "low");
            default         -> new ArrayList<>(AssetDto.STATUS_LABELS.keySet());
        };
        Map<String, String> options = new LinkedHashMap<>();
        keys.forEach(k -> options.put(k, AssetDto.STATUS_LABELS.get(k)));
        return options;
    }
}