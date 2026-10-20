package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 드론 기체의 고장 수리나 정기 점검 내역을 관리하는 테이블

@Entity
@Table(name = "maintenance_logs")
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaintenanceLogs {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drone_id", nullable = false)
    private Drones drone; // 정비 대상 드론(Drones) 참조

    @Column(nullable = false)
    private LocalDate maintenanceDate; // 정비 일자

    @Column(nullable = false, length = 500)
    private String description; // 상세 정비 내역 및 부품 교체 기록

    private Integer cost; // 정비 비용 (원)

    @Column(length = 50)
    private String mechanicName; // 정비 담당자명

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
