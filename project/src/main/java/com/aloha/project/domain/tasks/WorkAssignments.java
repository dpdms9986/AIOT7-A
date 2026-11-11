
package com.aloha.project.domain.tasks;

import java.time.LocalDateTime;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.users.Users;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 조종사별로 어떤 농지를 배정받았는지 기록하며, 작업 시작 시 비관적 락과 함께 상태가 갱신되는 핵심 테이블입니다.

@Entity
@Table(name = "work_assignments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkAssignments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_batches_no", nullable = false)
    private DispatchBatches dispatchBatch; // 상위 배정 묶음 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lands_no", nullable = false)
    private Lands land; // 배정된 대상 농지 참조

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "users_no", nullable = false)
    private Users pilot; // 담당 조종사(Users.no)

    private Double weightRatio; // 분배 가중치 비율 (균등 또는 수동 할당 비율)

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ASSIGNED";
    // ASSIGNED(배정됨), IN_PROGRESS(작업중),
    // PAUSED(일시정지), INTERRUPTED(강제중단),
    // STOPPED(작업중지/인계), COMPLETED(완료)

    @Column(name = "flight_path", columnDefinition = "TEXT")
    private String flightPath; // 드론 이동 경로

    @Column(name = "last_latitude")
    private Double lastLatitude; // 마지막 GPS 위도

    @Column(name = "last_longitude")
    private Double lastLongitude; // 마지막 GPS 경도

    @Column(name = "last_location_at")
    private LocalDateTime lastLocationAt; // 마지막 GPS 수신 일시

    @Column(name = "interrupted_at")
    private LocalDateTime interruptedAt; // 작업 중단 일시

    @Column(name = "reassignment_notice_read_at")
    private LocalDateTime reassignmentNoticeReadAt; // 재배정 알림 확인 일시

    private LocalDateTime startedAt; // 작업 시작 일시

    private LocalDateTime completedAt; // 작업 완료 일시

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Lands.java 및 WorkAssignments.java 내부에 각각 추가
    public void updateStatus(String status) {
        this.status = status;
    }
}
