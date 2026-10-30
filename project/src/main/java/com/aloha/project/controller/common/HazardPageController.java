package com.aloha.project.controller.common;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.tasks.HazardReports;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.HazardRequest;
import com.aloha.project.repository.common.HazardRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/hazards")
@RequiredArgsConstructor
public class HazardPageController {

    private final HazardRepository hazardRepository;
    private final UserRepository userRepository;

    /**
     * 1. 위험 구역 목록 화면 (단장/조종사 공통)
     */
    @GetMapping
    public String getHazardList(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        // 최신순 위험 목록 조회
        List<HazardReports> hazards = hazardRepository.findAllByOrderByCreatedAtDesc();

        // 상단 요약 통계 집계
        long totalCount = hazards.size();
        long activeCount = hazards.stream().filter(h -> "ACTIVE".equals(h.getStatus())).count(); // 주의 요망
        long resolvedCount = hazards.stream().filter(h -> "RESOLVED".equals(h.getStatus())).count(); // 해소됨
        long highRiskCount = hazards.stream().filter(h -> "고위험".equals(h.getHazardType()) && "ACTIVE".equals(h.getStatus())).count(); // 고위험

        model.addAttribute("hazards", hazards);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("highRiskCount", highRiskCount);

        return "page/hazards"; // templates/page/hazards.html
    }

    /**
     * 2. 위험 구역 신규 등록 (폼 제출)
     */
    @PostMapping
    public String createHazard(@ModelAttribute HazardRequest request,
                               @AuthenticationPrincipal CustomUser customUser,
                               RedirectAttributes redirectAttributes) {
        if (customUser == null) return "redirect:/login";

        try {
            Users author = userRepository.findById(customUser.getUser().getNo()).orElse(null);

            HazardReports hazard = HazardReports.builder()
                    .user(author)
                    .title(request.getTitle().trim())
                    .content(request.getContent().trim())
                    .hazardType(request.getHazardType() != null ? request.getHazardType() : "중위험")
                    .latitude(request.getLatitude() != null ? request.getLatitude() : 36.3504) // 기본값(대전)
                    .longitude(request.getLongitude() != null ? request.getLongitude() : 127.3845)
                    .status("ACTIVE")
                    .build();

            hazardRepository.save(hazard);
            log.info("위험 요소 등록 성공: {} (작성자: {})", hazard.getTitle(), author != null ? author.getName() : "");
            redirectAttributes.addFlashAttribute("successMessage", "위험 구역이 성공적으로 등록되었습니다.");

        } catch (Exception e) {
            log.error("위험 등록 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "위험 구역 등록 중 오류가 발생했습니다.");
        }

        return "redirect:/hazards";
    }

    /**
     * 3. 위험 해소 완료 처리 (버튼 클릭 시)
     */
    @PostMapping("/{no}/resolve")
    public String resolveHazard(@PathVariable("no") Long no, RedirectAttributes redirectAttributes) {
        HazardReports hazard = hazardRepository.findById(no).orElse(null);
        if (hazard != null) {
            hazard.setStatus("RESOLVED");
            hazardRepository.save(hazard);
            redirectAttributes.addFlashAttribute("successMessage", "위험 요소가 해소(완료) 처리되었습니다.");
        }
        return "redirect:/hazards";
    }
}