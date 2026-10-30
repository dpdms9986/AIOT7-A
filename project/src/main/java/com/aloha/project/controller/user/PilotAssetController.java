package com.aloha.project.controller.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aloha.project.dto.AssetDto;
import com.aloha.project.dto.AssetLogDto;

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
        List<AssetDto> all = sampleMyAssets();

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

    /* =========================================================
       샘플 데이터 (백엔드 연결 전까지만 사용)
       - 로그인한 조종사를 "김민수"라고 가정 → 김민수 담당 자산만
       ========================================================= */
    private List<AssetDto> sampleMyAssets() {
        List<AssetDto> list = new ArrayList<>();

        list.add(AssetDto.builder().id("DR-001").type("drone").name("DJI Agras T40")
                .subtitle("기체 코드 DR-001").owner("김민수").status("operating")
                .lastManagedAt(LocalDate.of(2026, 10, 20)).tankCapacity(40).batteryCycles(212)
                .logs(List.of(
                        new AssetLogDto(LocalDate.of(2026, 10, 20), "정기 점검", "프로펠러, 노즐 점검", "0원"),
                        new AssetLogDto(LocalDate.of(2026, 9, 2), "부품 교체", "배터리 1팩 교체", "380,000원")))
                .build());

        list.add(AssetDto.builder().id("DR-004").type("drone").name("DJI Agras T25")
                .subtitle("기체 코드 DR-004 (예비기)").owner("김민수").status("maintenance")
                .lastManagedAt(LocalDate.of(2026, 10, 26)).tankCapacity(20).batteryCycles(95)
                .logs(List.of(
                        new AssetLogDto(LocalDate.of(2026, 10, 26), "점검 요청", "노즐 막힘 의심", "-")))
                .build());

        list.add(AssetDto.builder().id("CH-001").type("chemical").name("A사 살균제")
                .subtitle("분류: 살균제").owner("김민수").status("normal")
                .lastManagedAt(LocalDate.of(2026, 10, 18)).stock(42.5)
                .logs(List.of(
                        new AssetLogDto(LocalDate.of(2026, 10, 18), "20L", "정기 입고", "")))
                .build());

        return list;
    }
}