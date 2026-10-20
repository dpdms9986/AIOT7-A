package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 위험 요소 게시글 하나에 여러 장의 현장 사진을 첨부할 수 있도록 1:N 구조로 매핑

@Entity
@Table(name = "hazard_images")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HazardImages {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hazard_reports_no", nullable = false)
    private HazardReports hazardReport; // 상위 위험 신고 게시글 참조

    @Column(nullable = false, length = 255)
    private String originalFileName; // 사용자가 올린 원본 파일명

    @Column(nullable = false, length = 500)
    private String storedFilePath; // 서버에 저장된 실제 파일 경로 또는 URL

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
