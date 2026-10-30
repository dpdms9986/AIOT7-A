package com.aloha.project.domain.tasks;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "work_assignment_resources")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkAssignmentResources {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    // 어떤 작업 배정 건에 선택된 자원인지 연결
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_assignments_no", nullable = false, unique = true)
    private WorkAssignments workAssignment;

    // 선택한 드론
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drones_no", nullable = false)
    private Drones drone;

    // 선택한 약제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemicals_no", nullable = false)
    private Chemicals chemical;

    // 선택 일시
    @Column(name = "selected_at", updatable = false)
    private LocalDateTime selectedAt;

    @PrePersist
    protected void onCreate() {
        this.selectedAt = LocalDateTime.now();
    }
}