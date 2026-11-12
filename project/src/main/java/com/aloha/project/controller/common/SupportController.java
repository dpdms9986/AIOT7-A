package com.aloha.project.controller.common;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.common.Inquiries;
import com.aloha.project.domain.common.Pagination;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.dto.common.InquiryDto;
import com.aloha.project.service.inter.common.InquiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 고객센터 (SUP_001 ~ SUP_007)
 * - 로그인한 사용자만 이용 가능
 * - 실제 inquiries 테이블과 연결
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/support")
public class SupportController {

    private final InquiryService inquiryService;

    /* =========================================================
       고객센터 메인 (자주 묻는 질문 + 안내)
       ========================================================= */
    @GetMapping
    public String getSupport(
            @AuthenticationPrincipal CustomUser customUser,
            Model model) {

        if (customUser == null) return "redirect:/login";

        // 내 문의 중 답변 대기 건수
        long waitingCount = inquiryService.myList(
                customUser.getUser().getNo()
        ).stream()
         .filter(inquiry -> !"COMPLETED".equals(inquiry.getStatus()))
         .count();

        model.addAttribute("waitingCount", waitingCount);

        return "page/common/support";
    }

    /* =========================================================
       내 문의내역 목록
       ========================================================= */
    @GetMapping("/my")
    public String getMyInquiries(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        if (customUser == null) return "redirect:/login";

        List<InquiryDto> myInquiries = inquiryService
                .myList(customUser.getUser().getNo())
                .stream()
                .map(this::toInquiryDto)
                .toList();

        // 페이지네이션 (10개씩)
        final int pageSize = 10;
        Pagination pagination = new Pagination(Math.max(page, 1), pageSize, 10, myInquiries.size());
        if (pagination.getPage() > pagination.getLast()) {
            pagination = new Pagination(pagination.getLast(), pageSize, 10, myInquiries.size());
        }
        int fromIdx = (int) Math.min(pagination.getIndex(), myInquiries.size());
        int toIdx   = Math.min(fromIdx + pageSize, myInquiries.size());

        model.addAttribute("myInquiries", myInquiries.subList(fromIdx, toIdx));
        model.addAttribute("myTotal", myInquiries.size());
        model.addAttribute("pagination", pagination);

        return "page/common/support-my";
    }

    /* =========================================================
       1:1 문의 등록
       POST /support
       ========================================================= */
    @PostMapping
    public String sendInquiry(
            @RequestParam("type") String type,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes redirectAttributes) {

        if (customUser == null) return "redirect:/login";

        if (title.isBlank() || content.isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "제목과 내용을 입력하세요."
            );
            return "redirect:/support";
        }

        // 실제 DB 저장용 Entity 생성
        Inquiries inquiry = Inquiries.builder()
                .user(customUser.getUser())
                .category(type)
                .title(title.trim())
                .content(content.trim())
                .status("WAITING")
                .build();

        boolean result = inquiryService.create(inquiry);

        if (!result) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "문의 등록에 실패했습니다. 다시 시도해 주세요."
            );
            return "redirect:/support";
        }

        log.info("고객센터 문의 등록 완료: userNo={}, inquiryNo={}",
                customUser.getUser().getNo(), inquiry.getNo());

        redirectAttributes.addFlashAttribute(
                "message", "문의가 접수되었습니다. 답변은 내 문의내역에서 확인할 수 있어요."
        );

        return "redirect:/support/my";
    }

    /* =========================================================
       내 문의 상세 (본인 문의만)
       ========================================================= */
    @GetMapping("/inquiries/{no}")
    public String getInquiryDetail(
            @PathVariable("no") Long no,
            @AuthenticationPrincipal CustomUser customUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (customUser == null) return "redirect:/login";

        Inquiries inquiry = inquiryService
                .myDetail(no, customUser.getUser().getNo())
                .orElse(null);

        if (inquiry == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "문의를 찾을 수 없습니다."
            );
            return "redirect:/support/my";
        }

        model.addAttribute("inquiry", toInquiryDto(inquiry));

        return "page/common/support-detail";
    }

    /* =========================================================
       Entity → 화면용 DTO 변환
       ========================================================= */
    private InquiryDto toInquiryDto(Inquiries inquiry) {

        return InquiryDto.builder()
                .no(inquiry.getNo())
                .type(inquiry.getCategory())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .authorNo(inquiry.getUser().getNo())
                .authorName(inquiry.getUser().getName())
                .status("COMPLETED".equals(inquiry.getStatus())
                        ? "ANSWERED"
                        : "WAITING")
                .answer(inquiry.getAnswerContent())
                .answeredAt(inquiry.getAnsweredAt())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
