package com.aloha.project.controller.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.dto.AssetCreateRequest;
import com.aloha.project.dto.AssetDto;
import com.aloha.project.dto.AssetLogDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 방제단장 - 드론·자산 관리
 * 예) /leader/assetlist?tab=drone&owner=김민수&id=DR-001
 */
@Slf4j
@Controller
@RequestMapping("/leader")
public class LeaderAssetController {

    @GetMapping("/assetlist")
    public String getAssetList(
            @RequestParam(value = "tab",     defaultValue = "all") String tab,
            @RequestParam(value = "type",    defaultValue = "") String type,
            @RequestParam(value = "owner",   defaultValue = "") String owner,
            @RequestParam(value = "status",  defaultValue = "") String status,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "id",      required = false) String id,
            Model model) {

        // TODO(백엔드): assetService.getTeamAssets(로그인 단장의 팀)
        List<AssetDto> all = sampleAssets();

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
                .filter(a -> owner.isEmpty()  || owner.equals(a.getOwner()))
                .filter(a -> status.isEmpty() || a.getStatus().equals(status))
                .filter(a -> kw.isEmpty()
                          || (a.getId() + " " + a.getName() + " " + a.getSubtitle() + " " + a.getOwner()).contains(kw))
                .toList();

        // 2) 요약 카드 (전체 기준)
        long droneCount       = all.stream().filter(AssetDto::isDrone).count();
        long chemicalCount    = all.size() - droneCount;
        long maintenanceCount = all.stream().filter(AssetDto::isMaintenanceNeeded).count();

        // 3) 상세로 보여 줄 자산 (주소에 id 가 있을 때만)
        AssetDto selected = (id == null) ? null
                : all.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);

        // 4) 셀렉트 옵션
        List<String> owners = all.stream().map(AssetDto::getOwner).distinct().sorted().toList();

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
        model.addAttribute("owners", owners);
        model.addAttribute("statusOptions", AssetDto.STATUS_LABELS);   // Map → 셀렉트 옵션
        // 검색값 유지
        model.addAttribute("type", type);
        model.addAttribute("owner", owner);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);

        return "page/leader/assetlist";
    }

    /**
     * 자산 등록
     * POST /leader/assetlist
     */
    @PostMapping("/assetlist")
    public String createAsset(@ModelAttribute AssetCreateRequest form,
                              RedirectAttributes redirectAttributes) {
        log.info("자산 등록 요청: {}", form);
        // TODO(백엔드): assetService.create(로그인 단장의 팀, form)
        redirectAttributes.addFlashAttribute("message",
                form.getName() + " 자산 등록 요청을 받았습니다. (샘플이라 목록에는 추가되지 않아요)");
        return "redirect:/leader/assetlist";
    }

    /**
     * 자산 상태 변경
     * POST /leader/assetlist/{id}/status  (status)
     */
    @PostMapping("/assetlist/{id}/status")
    public String changeStatus(@PathVariable("id") String id,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        log.info("자산 상태 변경 요청: {} → {}", id, status);
        // TODO(백엔드): assetService.changeStatus(로그인 단장의 팀, id, status)
        redirectAttributes.addAttribute("id", id);   // 돌아가서 같은 자산 상세 열기
        redirectAttributes.addFlashAttribute("message",
                id + " 상태를 '" + AssetDto.STATUS_LABELS.getOrDefault(status, status) + "'(으)로 변경 요청했습니다. (샘플)");
        return "redirect:/leader/assetlist";
    }

    /* =========================================================
       샘플 데이터 (백엔드 연결 전까지만 사용)
       ========================================================= */
    private List<AssetDto> sampleAssets() {
        List<AssetDto> list = new ArrayList<>();

        list.add(AssetDto.builder().id("DR-001").type("drone").name("DJI Agras T40")
                .subtitle("기체 코드 DR-001").owner("김민수").status("operating")
                .lastManagedAt(LocalDate.of(2026, 10, 20)).tankCapacity(40).batteryCycles(212)
                .logs(List.of(
                        new AssetLogDto(LocalDate.of(2026, 10, 20), "정기 점검", "프로펠러, 노즐 점검", "0원"),
                        new AssetLogDto(LocalDate.of(2026, 9, 2), "부품 교체", "배터리 1팩 교체", "380,000원")))
                .build());

        list.add(AssetDto.builder().id("DR-002").type("drone").name("DJI Agras T30")
                .subtitle("기체 코드 DR-002").owner("이서연").status("normal")
                .lastManagedAt(LocalDate.of(2026, 10, 12)).tankCapacity(30).batteryCycles(148)
                .logs(List.of(
                        new AssetLogDto(LocalDate.of(2026, 10, 12), "정기 점검", "모터, 펌프 점검", "0원")))
                .build());

        list.add(AssetDto.builder().id("DR-003").type("drone").name("XAG P100")
                .subtitle("기체 코드 DR-003").owner("최유진").status("fault")
                .lastManagedAt(LocalDate.of(2026, 10, 25)).tankCapacity(40).batteryCycles(301)
                .logs(List.of(
                        new AssetLogDto(LocalDate.of(2026, 10, 25), "고장 접수", "GPS 수신 불량", "-")))
                .build());

        list.add(AssetDto.builder().id("CH-001").type("chemical").name("A사 살균제")
                .subtitle("분류: 살균제").owner("김민수").status("normal")
                .lastManagedAt(LocalDate.of(2026, 10, 18)).stock(42.5)
                .logs(List.of(
                        new AssetLogDto(LocalDate.of(2026, 10, 18), "20L", "정기 입고", "")))
                .build());

        list.add(AssetDto.builder().id("CH-002").type("chemical").name("B사 살충제")
                .subtitle("분류: 살충제").owner("이서연").status("low")
                .lastManagedAt(LocalDate.of(2026, 10, 3)).stock(4.0)
                .logs(List.of(
                        new AssetLogDto(LocalDate.of(2026, 10, 3), "10L", "긴급 입고", "재고 부족")))
                .build());

        return list;
    }
}