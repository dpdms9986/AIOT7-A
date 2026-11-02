package com.aloha.project.controller.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.dto.InquiryDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 고객센터 (SUP_001 ~ SUP_007)
 * - 로그인한 사람(단장, 조종사)만 사용 → 로그인 안 했으면 로그인 화면으로
 * 예) /support                → 자주 묻는 질문 + 고객센터 안내 (1:1 문의는 팝업)
 *     /support/my             → 내 문의내역 목록
 *     /support/inquiries/3    → 3번 문의 상세 (본인 문의만)
 *
 * ※ 지금은 INQUIRIES 테이블이 없어서 서버 메모리(SAMPLE_STORE)에 임시 저장
 *   → 서버를 다시 켜면 사라짐. TODO(백엔드) 자리를 inquiryService 로 교체
 * ※ 답변 등록은 관리자 대시보드(다른 팀원 담당)에서 처리 → 그 전까지는 모두 "답변 대기"
 */
@Slf4j
@Controller
@RequestMapping("/support")
public class SupportController {

    // TODO(백엔드): INQUIRIES 테이블 + inquiryService 연결 후 삭제
    private static final List<InquiryDto> SAMPLE_STORE = new ArrayList<>();
    private static final AtomicLong SEQ = new AtomicLong(0);

    /* =========================================================
       고객센터 메인 (자주 묻는 질문 + 안내)
       ========================================================= */
    @GetMapping
    public String getSupport(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        // 내 문의내역 버튼 옆 숫자 (답변 대기 건수)
        model.addAttribute("waitingCount",
                myInquiries(customUser).stream().filter(q -> !q.isAnswered()).count());

        return "page/support";
    }

    /* =========================================================
       내 문의내역 목록
       ========================================================= */
    @GetMapping("/my")
    public String getMyInquiries(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        model.addAttribute("myInquiries", myInquiries(customUser));
        return "page/support-my";
    }

    /* =========================================================
       1:1 문의 보내기 (팝업 폼, 작성자는 로그인 계정으로 자동 저장)
       POST /support  (type, title, content)
       ========================================================= */
    @PostMapping
    public String sendInquiry(@RequestParam("type") String type,
                              @RequestParam("title") String title,
                              @RequestParam("content") String content,
                              @AuthenticationPrincipal CustomUser customUser,
                              RedirectAttributes redirectAttributes) {

        if (customUser == null) return "redirect:/login";

        if (title.isBlank() || content.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "제목과 내용을 입력하세요.");
            return "redirect:/support";
        }

        log.info("문의 접수: user={}, type={}, title={}", customUser.getUser().getNo(), type, title);

        // TODO(백엔드): inquiryService.create(로그인 사용자, type, title, content)
        InquiryDto saved = InquiryDto.builder()
                .no(SEQ.incrementAndGet())
                .type(type)
                .title(title.trim())
                .content(content.trim())
                .authorNo(customUser.getUser().getNo())
                .authorName(customUser.getUser().getName())
                .status("WAITING")
                .createdAt(LocalDateTime.now())
                .build();
        SAMPLE_STORE.add(saved);

        redirectAttributes.addFlashAttribute("message", "문의가 접수되었습니다. 답변은 내 문의내역에서 확인할 수 있어요.");
        return "redirect:/support/my";
    }

    /* =========================================================
       내 문의 상세 (본인 문의만)
       ========================================================= */
    @GetMapping("/inquiries/{no}")
    public String getInquiryDetail(@PathVariable("no") Long no,
                                   @AuthenticationPrincipal CustomUser customUser,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        if (customUser == null) return "redirect:/login";

        InquiryDto inquiry = myInquiries(customUser).stream()
                .filter(q -> q.getNo().equals(no))
                .findFirst()
                .orElse(null);

        if (inquiry == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "문의를 찾을 수 없습니다.");
            return "redirect:/support/my";
        }

        model.addAttribute("inquiry", inquiry);
        return "page/support-detail";
    }

    // 로그인한 사람의 문의 목록 (최신순)
    private List<InquiryDto> myInquiries(CustomUser customUser) {
        Long myNo = customUser.getUser().getNo();
        // TODO(백엔드): inquiryService.getMyInquiries(myNo)
        return SAMPLE_STORE.stream()
                .filter(q -> myNo.equals(q.getAuthorNo()))
                .sorted(Comparator.comparing(InquiryDto::getCreatedAt).reversed())
                .toList();
    }
}