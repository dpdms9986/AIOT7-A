package com.aloha.project.domain.common;

import java.time.LocalDateTime;

import com.aloha.project.domain.lands.Regions;
import com.aloha.project.domain.users.Users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // 👇 위험 구역 지번/도로명 주소 (카카오 주소검색 연동)
    @Column(length = 200)
    private String address;

    @Column(nullable = false, length = 50)
    private String hazardType; // "고위험", "중위험", "저위험"

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

    // 👇 위험 구역 내용 수정 메서드 (더티 체킹용)
    public void update(String title, String content, String hazardType, String address, Double latitude, Double longitude) {
        this.title = title;
        this.content = content;
        this.hazardType = hazardType;
        this.address = address;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
    }
}
