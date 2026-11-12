package com.aloha.project.dto.common;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 1:1 문의 한 건 (고객센터)
 * - 사용자: 작성(POST /support), 내 문의내역 목록, 상세에서 답변 확인
 * - 관리자: 전체 목록, 답변 등록 (관리자 대시보드 담당자가 사용)
 * ※ 백엔드 약속: INQUIRIES 테이블 연결 후 서비스는 List<InquiryDto> 로 넘겨준다
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryDto {

    private Long no;                   // 문의 번호
    private String type;               // account / field / drone / etc
    private String title;              // 제목
    private String content;            // 문의 내용
    private Long authorNo;             // 작성자 번호 (users.no)
    private String authorName;         // 작성자 이름
    private String status;             // WAITING(답변 대기) / ANSWERED(답변 완료)
    private String answer;             // 관리자 답변 (없으면 null)
    private LocalDateTime answeredAt;  // 답변 일시
    private LocalDateTime createdAt;   // 문의 일시

    /* ===== 화면용 getter ===== */

    public String getTypeLabel() {
        if (type == null) return "기타";
        return switch (type) {
            case "account" -> "계정 · 로그인";
            case "field"   -> "농지 · 작업 배정";
            case "drone"   -> "드론 · 약제";
            default        -> "기타";
        };
    }

    public boolean isAnswered() {
        return "ANSWERED".equals(status);
    }

    public String getStatusLabel() {
        return isAnswered() ? "답변 완료" : "답변 대기";
    }

    // style.css 의 sp-status.* 색
    public String getStatusClass() {
        return isAnswered() ? "answered" : "waiting";
    }
}