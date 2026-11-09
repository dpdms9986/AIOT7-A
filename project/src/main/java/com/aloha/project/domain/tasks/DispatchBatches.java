package com.aloha.project.domain.tasks;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;

// 단장이 드래그로 영역을 지정해 한 번에 작업을 지시할 때 생성되는 묶음 단위입니다.

@Entity
@Table(name = "dispatch_batches")
@Getter 
@Setter
@Builder
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DispatchBatches {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teams_no", nullable = false)
    private Teams team; // 어느 방제단의 배정 묶음인지 구분

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_no", nullable = false)
    private Users leader; // 배정을 지시한 단장(Users.no)

    @Column(nullable = false, length = 100)
    private String batchTitle; // 배정 묶음 명칭 (예: 어남동 1차 공동방제)

    private Double totalArea; // 선택된 총 방제 면적 합계

    private Integer assignedPilotCount; // 투입된 조종사 수

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}