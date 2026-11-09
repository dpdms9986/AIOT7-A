package com.aloha.project.domain.users;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter 
@Setter 
@Builder
@AllArgsConstructor 
@NoArgsConstructor
public class Users {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 시스템 고유 식별자 (PK)

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String id; // UUID (UK)

    // [정리 1] 로그인 이메일(아이디) -> DTO와 동일하게 'username'으로 통일
    @Column(name = "user_name", nullable = false, unique = true, length = 100)
    private String userName; 

    @Column(nullable = false)
    private String password; // BCrypt 암호화된 비밀번호

    // [정리 2] 사용자 실명 -> DTO와 동일하게 'name'으로 통일
    @Column(name = "name", nullable = false, length = 50)
    private String name; 

    // [정리 3] DTO의 phone(비상연락처) 저장을 위해 필드 추가
    @Column(length = 20)
    private String phone; 

    @Column(nullable = false, length = 20)
    private String role; // ADMIN / LEADER / PILOT 

    // 소속 방제단 매핑
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id")
    private Teams team; 

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    // [정리 4] 중복되었던 userAuths 제거하고 authList 하나로 통일!
    @Builder.Default
    @OneToMany(
        mappedBy = "user", 
        cascade = CascadeType.ALL, 
        fetch = FetchType.EAGER, 
        orphanRemoval = true
    )
    private List<UserAuth> authList = new ArrayList<>();

    // 시큐리티 권한 등록 편의 메서드
    public void addAuth(UserAuth userAuth) {
        userAuth.setUser(this);
        this.authList.add(userAuth);
    }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.id = UUID.randomUUID().toString(); // UUID 자동 생성
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}