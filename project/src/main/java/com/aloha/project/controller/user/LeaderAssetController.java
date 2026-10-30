package com.aloha.project.controller.user;

import java.util.ArrayList;
import java.util.List;

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

import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.AssetCreateRequest;
import com.aloha.project.dto.AssetDto;
import com.aloha.project.repository.tasks.DroneRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 방제단장 - 드론·자산 관리
 * 예) /leader/assetlist?tab=drone&owner=김민수&id=DR-001
 */
@Slf4j
@Controller
@RequestMapping("/leader")
@RequiredArgsConstructor
public class LeaderAssetController {

    private final DroneRepository droneRepository;
    private final UserRepository userRepository;

    @GetMapping("/assetlist")
    public String getAssetList(
            @RequestParam(value = "tab",     defaultValue = "all") String tab,
            @RequestParam(value = "type",    defaultValue = "") String type,
            @RequestParam(value = "owner",   defaultValue = "") String owner,
            @RequestParam(value = "status",  defaultValue = "") String status,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "id",      required = false) String id,
            @AuthenticationPrincipal CustomUser customUser, // 👈 로그인 단장 주입
            Model model) {

        if (customUser == null) return "redirect:/login";

        // [DB 연동 1] 로그인 단장 및 소속 방제단 조회
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        Teams myTeam = (leader != null) ? leader.getTeam() : null;
        final String inviteCode = (myTeam != null && myTeam.getInviteCode() != null) ? myTeam.getInviteCode() : "";

        // [DB 연동 2] 실제 우리 방제단의 드론 목록을 AssetDto 로 변환
        List<AssetDto> all = new ArrayList<>();
        if (!inviteCode.isEmpty()) {
            all = droneRepository.findAll().stream()
                    .filter(d -> d.getUser() != null 
                              && d.getUser().getTeam() != null 
                              && inviteCode.equals(d.getUser().getTeam().getInviteCode()))
                    .map(AssetDto::from) // 👈 Drones -> AssetDto 변환 메서드 활용
                    .toList();
        }

        // ========================================================
        // 👇 프론트엔드 팀원이 작성한 탭 필터링 & 통계 로직 (100% 보존!)
        // ========================================================

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
     * 자산 등록 (실제 DB 저장)
     */
    @PostMapping("/assetlist")
    public String createAsset(@ModelAttribute AssetCreateRequest form,
                              @AuthenticationPrincipal CustomUser customUser,
                              RedirectAttributes redirectAttributes) {
        log.info("자산 등록 요청: {}", form);

        if (customUser == null) return "redirect:/login";
        Users leader = userRepository.findById(customUser.getUser().getNo()).orElse(null);

        if (leader != null) {
            // 드론 엔티티 생성 및 저장
            Drones drone = new Drones();
            drone.setUser(leader); // 기본 소유자: 등록한 단장
            drone.setModelName(form.getName());
            drone.setSerialNumber(form.getSubtitle() != null && !form.getSubtitle().isBlank() 
                                  ? form.getSubtitle() 
                                  : "SN-" + System.currentTimeMillis());
            drone.setStatus(form.getStatus() != null ? form.getStatus() : "AVAILABLE");
            drone.setTankCapacity(40.0); // 기본값
            drone.setBatteryCycle(0);

            droneRepository.save(drone);
            redirectAttributes.addFlashAttribute("message", form.getName() + " 자산이 성공적으로 등록되었습니다.");
        }

        return "redirect:/leader/assetlist";
    }

    /**
     * 자산 상태 변경 (실제 DB 변경)
     */
    @PostMapping("/assetlist/{id}/status")
    public String changeStatus(@PathVariable("id") String id,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        log.info("자산 상태 변경 요청: {} → {}", id, status);

        // 시리얼번호 또는 ID로 드론을 찾아 상태 업데이트
        Drones drone = droneRepository.findAll().stream()
                .filter(d -> id.equals(d.getSerialNumber()) || id.equals(String.valueOf(d.getNo())))
                .findFirst()
                .orElse(null);

        if (drone != null) {
            drone.setStatus(status);
            droneRepository.save(drone);
            redirectAttributes.addFlashAttribute("message", 
                    drone.getModelName() + " 상태가 '" + AssetDto.STATUS_LABELS.getOrDefault(status, status) + "'(으)로 변경되었습니다.");
        }

        redirectAttributes.addAttribute("id", id);
        return "redirect:/leader/assetlist";
    }
}