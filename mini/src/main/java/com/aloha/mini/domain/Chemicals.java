package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 방제단별로 보유한 농약/약제 품목과 현재고, 안전 재고 기준량을 관리

@Entity
@Table(name = "chemicals")
@Getter
@Setter
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chemicals {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chemicalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Teams team; // 소속 방제단별 약제 창고 격리

    @Column(nullable = false, length = 100)
    private String chemicalName; // 약제 품목명

    @Column(nullable = false, length = 50)
    private String category; // 약제 분류 (살충제 / 살균제 / 제초제) - AST_006

    private Double recommendedDosage; // 면적당 권장 살포량 - AST_006

    @Column(length = 100)
    private String manufacturer; // 제조사

    @Column(nullable = false)
    private Double currentStock; // 현재 재고 수량

    @Column(nullable = false, length = 20)
    private String unit; // 단위 (예: L, ml, 포, 병)

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
