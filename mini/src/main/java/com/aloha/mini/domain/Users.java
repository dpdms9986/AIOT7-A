package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 시스템 고유 식별자 (PK)

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String id; // UUID (UK)

    @Column(nullable = false, unique = true, length = 100)
    private String user_id; // 로그인 이메일 (UK)

    @Column(nullable = false)
    private String password; // BCrypt 암호화 비밀번호

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName; // 사용자 실명

    @Column(nullable = false, length = 20)
    private String role; // ADMIN / PILOT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Teams team; // 소속 방제단 매핑 (기존 유지)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 계정 생성 일시

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 계정 수정 일시

    @PrePersist
    protected void onCreate() {
        this.id = UUID.randomUUID().toString(); // Insert 시 UUID 자동 생성
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(); // Update 시 시간 자동 갱신
    }
}
