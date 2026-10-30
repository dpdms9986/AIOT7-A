package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.List;

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

        // 1) 탭 + 검색 조건으로 거르기
        String kw = keyword.trim();
        List<AssetDto> assets = all.stream()
                .filter(a -> switch (tab) {
                    case "drone"       -> a.isDrone();
                    case "chemical"    -> !a.isDrone();
                    case "maintenance" -> a.isMaintenanceNeeded();
                    default            -> true;
                })
                .filter(a -> type.isEmpty()   || a.getType().equals(type))
                .filter(a -> status.isEmpty() || a.getStatus().equals(status))
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
            case "drone"       -> "드론 기체";
            case "chemical"    -> "농업용 약제";
            case "maintenance" -> "정비 현황";
            default            -> "전체 자산";
        };

        model.addAttribute("tab", tab);
        model.addAttribute("tabTitle", tabTitle);
        model.addAttribute("assets", assets);
        model.addAttribute("droneCount", droneCount);
        model.addAttribute("chemicalCount", chemicalCount);
        model.addAttribute("maintenanceCount", maintenanceCount);
        model.addAttribute("selected", selected);
        model.addAttribute("statusOptions", AssetDto.STATUS_LABELS);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);

        return "page/pilot/assetlist";
    }
}