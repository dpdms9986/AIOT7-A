package com.aloha.project.controller.common;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.users.CustomUser;

import lombok.extern.slf4j.Slf4j;

/**
 * 고객센터 - 자주 묻는 질문 + 문의하기
 */
@Slf4j
@Controller
@RequestMapping("/support")
public class SupportController {

    @GetMapping
    public String getSupport(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) return "redirect:/login";
        return "page/support";
    }

    /**
     * 문의 보내기
     * POST /support  (name, email, type, content)
     */
    @PostMapping
    public String sendInquiry(@RequestParam("name") String name,
                              @RequestParam("email") String email,
                              @RequestParam("type") String type,
                              @RequestParam("content") String content,
                              @AuthenticationPrincipal CustomUser customUser,
                              RedirectAttributes redirectAttributes) {
        if (customUser == null) return "redirect:/login";

        log.info("문의 접수: name={}, email={}, type={}", name, email, type);

        // TODO(백엔드): 문의 저장 또는 관리자 메일 발송 (EmailService)
        redirectAttributes.addFlashAttribute("message", "문의가 접수되었습니다. 확인 후 이메일로 답변드릴게요. (샘플)");
        return "redirect:/support";
    }
}