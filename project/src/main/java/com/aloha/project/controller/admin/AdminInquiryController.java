package com.aloha.project.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aloha.project.domain.common.Inquiries;
import com.aloha.project.domain.common.Pagination;
import com.aloha.project.dto.InquiryDto;
import com.aloha.project.service.inter.common.InquiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 문의현황 (고객센터에서 단장/조종사가 남긴 1:1 문의)
 * GET /admin/inquiries?status=&category=&keyword=&page=1&size=10
 * 답변 등록은 화면 JS 가 기존 API 호출: POST /api/inquiries/admin/{no}/answer
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/inquiries")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public String list(
            @RequestParam(value = "status", defaultValue = "") String status,      // WAITING / COMPLETED / 전체(빈값)
            @RequestParam(value = "category", defaultValue = "") String category,   // account / field / drone / etc
            @RequestParam(value = "keyword", defaultValue = "") String keyword,     // 작성자 이름 또는 제목
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        page = Math.max(page, 1);
        size = (size < 1 || size > 100) ? 10 : size;

        // TODO(백엔드) 지금은 InquiryService#adminList 그대로 사용 (상태/유형/검색어 필터 + 최신순)
        Page<Inquiries> result = inquiryService.adminList(status, category, keyword.trim(), PageRequest.of(page - 1, size));

        // 없는 페이지 번호로 들어오면 마지막 페이지로 다시 조회
        if (result.getTotalPages() > 0 && page > result.getTotalPages()) {
            page = result.getTotalPages();
            result = inquiryService.adminList(status, category, keyword.trim(), PageRequest.of(page - 1, size));
        }

        // 요약 카드 (필터와 상관없는 전체 기준)
        long waitingCount = inquiryService.countWaiting();
        long totalCount = inquiryService.adminList(null, null, null, PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("inquiries", result.getContent().stream().map(this::toInquiryDto).toList());
        model.addAttribute("pagination", new Pagination(page, size, 10, result.getTotalElements()));
        model.addAttribute("status", status);
        model.addAttribute("category", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("waitingCount", waitingCount);
        model.addAttribute("completedCount", totalCount - waitingCount);

        return "page/admin/inquiries";
    }

    /** Entity → 화면용 DTO (고객센터 SupportController 와 같은 방식) */
    private InquiryDto toInquiryDto(Inquiries inquiry) {
        return InquiryDto.builder()
                .no(inquiry.getNo())
                .type(inquiry.getCategory())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .authorNo(inquiry.getUser().getNo())
                .authorName(inquiry.getUser().getName())
                .status("COMPLETED".equals(inquiry.getStatus()) ? "ANSWERED" : "WAITING")
                .answer(inquiry.getAnswerContent())
                .answeredAt(inquiry.getAnsweredAt())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}