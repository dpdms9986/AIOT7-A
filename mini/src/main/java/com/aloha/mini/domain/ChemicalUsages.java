package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 조종사가 [작업 완료]를 누를 때 사용한 약제량을 기록하며, 이때 Chemicals의 currentStock이 함께 차감

@Entity
@Table(name = "chemical_usages")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChemicalUsages {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_assignments_no", nullable = false)
    private WorkAssignments workAssignment; // 어떤 농지 배정 작업에서 썼는지 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemicals_no", nullable = false)
    private Chemicals chemical; // 사용한 약제 품목 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_no", nullable = false)
    private Users user; // 약제를 사용한 조종사(Users.no)

    @Column(nullable = false)
    private Double usedAmount; // 실제 소모량

    @Column(name = "used_at", updatable = false)
    private LocalDateTime usedAt; // 사용(작업 완료) 일시

    @PrePersist
    protected void onCreate() {
        this.usedAt = LocalDateTime.now();
    }
}
