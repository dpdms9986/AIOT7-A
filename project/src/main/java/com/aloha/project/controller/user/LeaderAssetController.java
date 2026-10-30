package com.aloha.project.controller.user;

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
        List<AssetDto> all = new ArrayList<>();   // TODO(백엔드)

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

}