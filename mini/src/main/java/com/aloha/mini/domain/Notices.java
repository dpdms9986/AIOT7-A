package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 단장이 소속 팀원들에게 공지사항이나 긴급 기상특보를 전달하는 테이블

@Entity
@Table(name = "notices")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notices {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teams_no", nullable = false)
    private Teams team; // 어느 방제단의 공지사항인지 구분

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_no", nullable = false)
    private Users user; // 작성한 단장(Users.no)

    @Column(nullable = false, length = 150)
    private String title; // 공지 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 공지 본문

    @Column(nullable = false)
    private Boolean isPinned = false; // 상단 고정 및 긴급 기상특보 배너 노출 여부

    @Column(nullable = false)
    private Integer viewCount = 0; // 조회수

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
