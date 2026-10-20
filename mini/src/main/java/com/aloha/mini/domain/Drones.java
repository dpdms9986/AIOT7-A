package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 조종사(또는 단장)가 운용하는 드론 기체 정보입니다. 
// 소유자인 Users를 통해 소속 방제단(Teams)이 자연스럽게 식별

@Entity
@Table(name = "drones")
@Getter
@Setter 
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drones {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long droneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private Users user; // 기체 담당자/소유자(Users.no)

    @Column(nullable = false, length = 100)
    private String modelName; // 기체 모델명 (예: DJI Agras T40)

    @Column(nullable = false, unique = true, length = 100)
    private String serialNumber; // 기체 고유 시리얼 번호

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE"; // AVAILABLE(사용 가능) / IN_USE (비행 중 ) / MAINTENANCE ( 점검 중 )

    @Column(nullable = false)
    private Double tankCapacity; // 약제 탱크 용량 (단위: L) - AST_001, ADM_019 필수 데이터

    private Integer batteryCycle = 0; // 배터리 사이클 횟수 - AST_002, AST_003

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
