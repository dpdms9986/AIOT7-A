package com.aloha.mini.domain; // 패키지 경로에 맞게 수정

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_task")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskNo;

    private Long teamNo;          // 방제단 번호
    private String district;      // 행정구역
    private String address;       // 상세 주소
    private String pesticide;     // 약제 종류
    
    private Long pilotUserNo;     // 담당 조종사 회원번호 (내 작업 조회용)
    private String pilotName;     // 담당 조종사 이름 (화면 표시용)

    private String status;        // ASSIGNED(배정), IN_PROGRESS(비행중), COMPLETED(완료)

    private LocalDateTime assignedAt;  // 배정일시
    private LocalDateTime completedAt; // 완료일시

    // 처음 저장될 때 배정일시와 초기 상태를 자동으로 세팅
    @PrePersist
    public void prePersist() {
        this.assignedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ASSIGNED";
        }
    }
}