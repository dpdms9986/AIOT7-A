package com.aloha.project.controller.common;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 마이페이지 (GNB_007, USER_011)
 * - 로그인한 사람(단장, 조종사) 본인 정보만 보고 수정
 * 예) /mypage                 → 내 정보 + 방제 실적
 *     POST /mypage/profile    → 연락처 수정
 *     POST /mypage/password   → 비밀번호 변경
 *
 * ※ 이미 있는 UserRepository, WorkAssignmentRepository 만 사용 (새 테이블 없음)
 */
@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserRepository userRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    /* =========================================================
       내 정보 + 방제 실적
       ========================================================= */
    @Transactional(readOnly = true)
    @GetMapping
    public String getMyPage(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        Users me = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        if (me == null) return "redirect:/login";

        // 방제 실적 (조종사: 내 작업 기준)
        long totalDone = 0, monthDone = 0, inProgress = 0, waiting = 0;
        if ("PILOT".equals(me.getRole())) {
            List<WorkAssignments> mine = workAssignmentRepository.findByPilot_No(me.getNo());
            LocalDate firstDay = LocalDate.now().withDayOfMonth(1);

            totalDone  = mine.stream().filter(w -> "COMPLETED".equals(w.getStatus())).count();
            monthDone  = mine.stream()
                    .filter(w -> "COMPLETED".equals(w.getStatus()))
                    .filter(w -> w.getCompletedAt() != null
                              && !w.getCompletedAt().toLocalDate().isBefore(firstDay))
                    .count();
            inProgress = mine.stream().filter(w -> "IN_PROGRESS".equals(w.getStatus())).count();
            waiting    = mine.stream().filter(w -> "ASSIGNED".equals(w.getStatus())).count();
        }

        // 단장: 우리 방제단 조종사 수
        long pilotCount = 0;
        if ("LEADER".equals(me.getRole()) && me.getTeam() != null) {
            pilotCount = userRepository.findByTeam(me.getTeam()).stream()
                    .filter(u -> "PILOT".equals(u.getRole()))
                    .count();
        }

        model.addAttribute("me", me);
        model.addAttribute("teamName", me.getTeam() != null ? me.getTeam().getTeamName() : "소속 없음");
        model.addAttribute("isPilot", "PILOT".equals(me.getRole()));
        model.addAttribute("totalDone", totalDone);
        model.addAttribute("monthDone", monthDone);
        model.addAttribute("inProgress", inProgress);
        model.addAttribute("waiting", waiting);
        model.addAttribute("pilotCount", pilotCount);

        return "page/common/mypage";
    }

    /* =========================================================
       연락처 수정 (본인만)
       ========================================================= */
    @PostMapping("/profile")
    public String updateProfile(@RequestParam("phone") String phone,
                                @AuthenticationPrincipal CustomUser customUser,
                                RedirectAttributes redirectAttributes) {
        if (customUser == null) return "redirect:/login";

        String p = phone.trim();
        if (!p.isEmpty() && !p.matches("^01[0-9]-?\\d{3,4}-?\\d{4}$")) {
            redirectAttributes.addFlashAttribute("errorMessage", "연락처 형식을 확인해 주세요. (예: 010-1234-5678)");
            return "redirect:/mypage";
        }

        Users me = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        if (me == null) return "redirect:/login";

        me.setPhone(p.isEmpty() ? null : p);
        userRepository.save(me);
        log.info("연락처 수정: user={}", me.getNo());

        redirectAttributes.addFlashAttribute("message", "연락처가 수정되었습니다.");
        return "redirect:/mypage";
    }

    /* =========================================================
       비밀번호 변경 (현재 비밀번호 확인 후)
       ========================================================= */
    @PostMapping("/password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("newPasswordConfirm") String newPasswordConfirm,
                                 @AuthenticationPrincipal CustomUser customUser,
                                 RedirectAttributes redirectAttributes) {
        if (customUser == null) return "redirect:/login";

        Users me = userRepository.findById(customUser.getUser().getNo()).orElse(null);
        if (me == null) return "redirect:/login";

        if (!passwordEncoder.matches(currentPassword, me.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "현재 비밀번호가 맞지 않습니다.");
            return "redirect:/mypage";
        }
        if (newPassword.length() < 4) {
            redirectAttributes.addFlashAttribute("errorMessage", "새 비밀번호는 4자 이상으로 입력해 주세요.");
            return "redirect:/mypage";
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            redirectAttributes.addFlashAttribute("errorMessage", "새 비밀번호와 확인이 일치하지 않습니다.");
            return "redirect:/mypage";
        }

        me.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(me);
        log.info("비밀번호 변경: user={}", me.getNo());

        redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        return "redirect:/mypage";
    }
}