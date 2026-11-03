package com.aloha.project.domain.tasks;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.aloha.project.domain.users.Users;

// 조종사(또는 단장)가 운용하는 드론 기체 정보입니다. 
// 소유자인 Users를 통해 소속 방제단(Teams)이 자연스럽게 식별

@Entity
@Table(name = "drones")
@Getter
@Setter 
@AllArgsConstructor 
@NoArgsConstructor
public class Drones {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drone_id")
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_no", nullable = false)
    private Users user; // 기체 담당자/소유자(Users.no)

    @Column(nullable = false, length = 100)
    private String modelName; // 기체 모델명 (예: DJI Agras T40)

    // @Column(nullable = false, unique = true, length = 100)
    // private String serialNumber; // 기체 고유 시리얼 번호

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE"; // AVAILABLE(사용 가능) / IN_USE (비행 중 ) / MAINTENANCE ( 점검 중 )

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
