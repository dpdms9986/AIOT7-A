
package com.aloha.project.domain.tasks;

import java.time.LocalDateTime;

import com.aloha.project.domain.users.Teams;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chemicals")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chemicals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teams_no", nullable = false)
    private Teams team; // 소속 방제단

    @Column(name = "chemical_name", nullable = false, length = 100)
    private String chemicalName; // 약제 품목명

    @Column(nullable = false, length = 50)
    private String category; // 살충제 / 살균제 / 제초제 / 영양제

    @Column(length = 100)
    private String manufacturer; // 제조사

    // @Column(nullable = false)
    // private Double currentStock = 0.0; // 현재 재고량

    // @Column(nullable = false)
    // private Double safetyStock; // 안전 재고 기준량

    // @Column(nullable = false, length = 20)
    // private String unit = "L"; // 재고 관리 단위

    @Builder.Default
    @Column(nullable = false, length = 20)  // 재고량 상태
    private String status = "normal";       // normal: 양호, low: 부족

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
