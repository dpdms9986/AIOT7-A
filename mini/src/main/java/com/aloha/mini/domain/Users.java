package com.aloha.mini.domain;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 시스템 고유 식별자 (PK)

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String id; // UUID (UK)

    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId; // 로그인 이메일 (UK)

    @Column(nullable = false)
    private String password; // BCrypt 암호화 비밀번호

    @Column(name = "user_name", nullable = false, length = 50)
    private String username; // 사용자 실명

    @Column(nullable = false, length = 20)
    private String role; // ADMIN /LEADER / PILOT 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Teams team; // 소속 방제단 매핑 (기존 유지)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 계정 생성 일시

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 계정 수정 일시

    @Builder.Default              // 빌더 패턴으로 객체 생성 시, 초기화한 값을 넣어준다.
    @Column(nullable = false)
    private int enabled = 1;

  // 회원 권한 목록 (Users : UserAuth = 1 : N)
  // cascade = CascadeType.ALL  : ON UPDATE CASCADE
  // ➡ 부모 Users 의 PK(no) 변경되면, 자식 UserAuth FK(user_no) 변경합니다.

  // fetch = FetchType.LAZY     : ON UPDATE CASCADE
  // ➡ 부모 Users 의 데이터가 삭제되면, 자식 UserAuth 의 데이터도 삭제합니다.
  // fetch = FetchType.LAZY
  // ➡ LAZY : Users 를 조회해도 아직 List<UserAuth> 조회되지 않는다.
  //           authList 를 사용하려고 하면 그때 List<UserAuth> 을 조회한다.
  // ➡ EAGER : Users 조회할 때 바로 List<UserAuth> 을 조회한다.

  // orphanRemoval = true : 부모 자식 관계가 끊기면, 자식 UserAuth 데이터를 삭제한다.
  @Builder.Default
  @OneToMany(
    mappedBy = "user", cascade = CascadeType.ALL, 
    fetch = FetchType.EAGER, orphanRemoval = true
  )
  private List<UserAuth> authList = new ArrayList<>();

  // 시큐리티 인가를 위해 List<UserAuth> authList에도 넣는 구조 (addAuth 메섣 활용)
  public void addAuth(UserAuth userAuth) {
    userAuth.setUser(this);
    this.authList.add(userAuth);
  }

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
