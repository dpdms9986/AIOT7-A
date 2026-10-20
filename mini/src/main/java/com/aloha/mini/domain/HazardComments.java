package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 동료 조종사들이 위험 정보에 대해 추가 의견을 남기는 댓글 테이블

@Entity
@Table(name = "hazard_comments")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HazardComments {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private HazardReports hazardReport; // 대상 위험 신고 게시글 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private Users user; // 댓글 작성자(Users.no)

    @Column(nullable = false, length = 500)
    private String content; // 댓글 내용

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
