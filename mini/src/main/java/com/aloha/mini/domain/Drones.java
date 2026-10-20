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
    private String status = "READY"; // READY(정상대기), IN_FLIGHT(비행중), MAINTENANCE(정비중)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
