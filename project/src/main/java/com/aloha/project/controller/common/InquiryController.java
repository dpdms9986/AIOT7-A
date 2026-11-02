package com.aloha.project.controller.common;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.common.Inquiries;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.InquiryCreateRequest;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.common.InquiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final UserRepository userRepository;

    /**
     * 고객센터 1:1 문의 등록
     * - 로그인한 방제단장(LEADER), 조종사(PILOT)만 작성 가능
     * - 문의 내용 DB 저장
     */
    @PostMapping("/support")
    public String createInquiry(
            @ModelAttribute InquiryCreateRequest request,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "type", required = false) String type,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes redirectAttributes) {

        // 1. 로그인 여부 확인
        if (customUser == null) {
            return "redirect:/login";
        }

        try {
            // 2. 로그인 회원 정보 조회
            Users user = userRepository.findById(customUser.getUser().getNo())
                    .orElse(null);

            if (user == null) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "회원 정보를 확인할 수 없습니다.");
                return "redirect:/support";
            }

            // 3. 문의 작성 권한 확인
            String role = user.getRole();

            if (!"LEADER".equals(role) && !"PILOT".equals(role)) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "문의 작성 권한이 없습니다.");
                return "redirect:/support";
            }

            // 4. DTO 필드명 지원
            String replyEmail = (email != null) ? email : request.getReplyEmail();
            String category = (type != null) ? type : request.getCategory();

            // 5. 필수 입력값 확인
            if (isBlank(request.getName())
                    || isBlank(replyEmail)
                    || isBlank(category)
                    || isBlank(request.getContent())) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage", "모든 문의 항목을 입력해 주세요.");
                return "redirect:/support";
            }

            // 6. 문의 유형 확인
            if (!isValidCategory(category)) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "올바른 문의 유형을 선택해 주세요.");
                return "redirect:/support";
            }

            // 7. 문의 Entity 생성
            Inquiries inquiry = Inquiries.builder()
                    .user(user)
                    .name(request.getName().trim())
                    .replyEmail(replyEmail.trim())
                    .category(category.trim())
                    .content(request.getContent().trim())
                    .build();

            // 8. 문의 저장
            boolean isSuccess = inquiryService.create(inquiry);

            if (!isSuccess) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "문의 접수에 실패했습니다. 다시 시도해 주세요.");
                return "redirect:/support";
            }

            log.info("고객센터 문의 접수 성공: no={}, 작성자={}",
                    inquiry.getNo(), user.getNo());

            redirectAttributes.addFlashAttribute(
                    "successMessage", "문의가 정상적으로 접수되었습니다.");

        } catch (Exception e) {
            log.error("고객센터 문의 처리 중 오류 발생", e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "문의 접수 중 오류가 발생했습니다.");
        }

        return "redirect:/support";
    }

    /**
     * 문자열 공백 확인
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 문의 유형 확인
     */
    private boolean isValidCategory(String category) {
        return "account".equals(category)
                || "field".equals(category)
                || "drone".equals(category)
                || "etc".equals(category);
    }
}