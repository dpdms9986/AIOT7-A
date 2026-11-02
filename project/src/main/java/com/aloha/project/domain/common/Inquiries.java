
package com.aloha.project.domain.common;

import java.time.LocalDateTime;

import com.aloha.project.domain.users.Users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 고객센터 1:1 문의 및 관리자 답변 관리

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

    // 문의 유형 (account / field / drone / etc)
    @Column(nullable = false, length = 50)
    private String category;

    // 문의 제목
    @Column(nullable = false, length = 200)
    private String title;

    // 문의 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 문의 상태 (WAITING / COMPLETED)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "WAITING";

    // 관리자 답변 내용
    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    // 관리자 답변 등록 일시
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    // 문의 등록 일시
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = "WAITING";
        }
    }
}
