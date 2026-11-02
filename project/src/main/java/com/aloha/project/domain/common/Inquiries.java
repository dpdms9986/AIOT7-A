package com.aloha.project.domain.common;

import java.time.LocalDateTime;

import com.aloha.project.domain.users.Users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Entity
@Table(name = "inquiries")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Inquiries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    // 문의 작성자 (Users.no)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_no", nullable = false)
    private Users user;

    // 문의자 이름
    @Column(nullable = false, length = 100)
    private String name;

    // 답변 수신 이메일
    @Column(name = "reply_email", nullable = false, length = 255)
    private String replyEmail;

    // 문의 유형
    // account / field / drone / etc
    @Column(nullable = false, length = 50)
    private String category;

    // 문의 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 문의 접수 일시
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}