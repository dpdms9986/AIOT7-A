
package com.aloha.project.api.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.common.Inquiries;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.dto.common.InquiryAnswerRequest;
import com.aloha.project.dto.common.InquiryCreateRequest;
import com.aloha.project.service.inter.common.InquiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryApi {

    private final InquiryService inquiryService;

    // 문의 유형 코드
    private static final Set<String> CATEGORIES =
            Set.of("account", "field", "drone", "etc");

    /**
     * SUP_005
     * 1:1 문의 등록
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody InquiryCreateRequest request,
            @AuthenticationPrincipal CustomUser customUser) {

        // 로그인 확인
        if (customUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        // 필수 입력값 확인
        if (request.getCategory() == null
                || !CATEGORIES.contains(request.getCategory())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "올바른 문의 유형을 선택해주세요."));
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "문의 제목을 입력해주세요."));
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "문의 내용을 입력해주세요."));
        }

        // 로그인한 사용자를 작성자로 지정
        Inquiries inquiry = Inquiries.builder()
                .user(customUser.getUser())
                .category(request.getCategory())
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .status("WAITING")
                .build();

        boolean result = inquiryService.create(inquiry);

        if (!result) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "문의 등록에 실패했습니다."));
        }

        log.info("문의 등록 성공 - 문의 번호: {}", inquiry.getNo());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("no", inquiry.getNo());
        response.put("message", "문의가 등록되었습니다.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * SUP_006
     * 로그인한 사용자의 문의 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<?> myList(
            @AuthenticationPrincipal CustomUser customUser) {

        if (customUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        List<Map<String, Object>> response = inquiryService
                .myList(customUser.getUser().getNo())
                .stream()
                .map(this::toMyListResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * SUP_007
     * 본인이 작성한 문의 상세 및 답변 조회
     */
    @GetMapping("/my/{no}")
    public ResponseEntity<?> myDetail(
            @PathVariable("no") Long no,
            @AuthenticationPrincipal CustomUser customUser) {

        if (customUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        Inquiries inquiry = inquiryService
                .myDetail(no, customUser.getUser().getNo())
                .orElse(null);

        // 본인 문의가 아니거나 존재하지 않는 경우
        if (inquiry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "문의 내역을 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(toMyDetailResponse(inquiry));
    }

    /**
     * SUP_008
     * 관리자 대시보드 답변 대기 문의 건수
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/waiting-count")
    public ResponseEntity<?> waitingCount() {

        long count = inquiryService.countWaiting();

        return ResponseEntity.ok(
                Map.of("waitingCount", count)
        );
    }

    /**
     * SUP_009
     * 관리자 전체 문의 목록 조회
     *
     * status   : WAITING / COMPLETED
     * category : account / field / drone / etc
     * keyword  : 작성자 이름 또는 문의 제목
     * page     : 페이지 번호 (0부터 시작)
     * size     : 페이지당 조회 건수
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<?> adminList(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        // 페이지 요청값 확인
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "올바르지 않은 페이지 요청입니다."));
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Inquiries> inquiries = inquiryService.adminList(
                status,
                category,
                keyword,
                pageable
        );

        Page<Map<String, Object>> response =
                inquiries.map(this::toAdminListResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * SUP_010
     * 관리자 문의 답변 등록
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{no}/answer")
    public ResponseEntity<?> answer(
            @PathVariable("no") Long no,
            @RequestBody InquiryAnswerRequest request) {

        if (request.getAnswerContent() == null
                || request.getAnswerContent().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "답변 내용을 입력해주세요."));
        }

        boolean result = inquiryService.answer(
                no,
                request.getAnswerContent()
        );

        if (!result) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "답변 등록에 실패했습니다. 문의가 존재하는지 또는 이미 답변 완료되었는지 확인해주세요."
                    ));
        }

        log.info("문의 답변 등록 성공 - 문의 번호: {}", no);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "답변이 등록되었습니다."
                )
        );
    }

    /**
     * 내 문의 목록 응답 데이터
     */
    private Map<String, Object> toMyListResponse(Inquiries inquiry) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("no", inquiry.getNo());
        response.put("category", inquiry.getCategory());
        response.put("title", inquiry.getTitle());
        response.put("createdAt", inquiry.getCreatedAt());
        response.put("status", inquiry.getStatus());

        return response;
    }

    /**
     * 내 문의 상세 응답 데이터
     */
    private Map<String, Object> toMyDetailResponse(Inquiries inquiry) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("no", inquiry.getNo());
        response.put("category", inquiry.getCategory());
        response.put("title", inquiry.getTitle());
        response.put("content", inquiry.getContent());
        response.put("status", inquiry.getStatus());
        response.put("createdAt", inquiry.getCreatedAt());
        response.put("answerContent", inquiry.getAnswerContent());
        response.put("answeredAt", inquiry.getAnsweredAt());

        return response;
    }

    /**
     * 관리자 문의 목록 응답 데이터
     */
    private Map<String, Object> toAdminListResponse(Inquiries inquiry) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("no", inquiry.getNo());
        response.put("userName", inquiry.getUser().getName());
        response.put("category", inquiry.getCategory());
        response.put("title", inquiry.getTitle());
        response.put("createdAt", inquiry.getCreatedAt());
        response.put("status", inquiry.getStatus());

        return response;
    }
}
