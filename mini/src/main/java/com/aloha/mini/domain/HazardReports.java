package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 고압선, 철탑 등 비행 위험 요소를 위치 좌표와 함께 공유하는 게시판

@Builder 
@Entity
@Table(name = "hazard_reports")
@Getter 
@Setter
@AllArgsConstructor 
@NoArgsConstructor
public class HazardReports {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_no", nullable = false)
    private Users user; // 신고 작성자(Users.no)

    @Column(nullable = false, length = 150)
    private String title; // 게시글 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 상세 위험 내용

    @Column(nullable = false, length = 50)
    private String hazardType; // 위험 유형 (예: 고압선, 통신철탑, 맹견, 비행금지구역)

    @Column(nullable = false)
    private Double latitude; // 위험 지점 위도 (관제 지도 빨간 마커 표시용)

    @Column(nullable = false)
    private Double longitude; // 위험 지점 경도 (관제 지도 빨간 마커 표시용)

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE(주의 요망), RESOLVED(해소됨)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_no")
    private Regions region; // 행정구역별 필터링용 매핑 - BRD_002, BRD_004

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
