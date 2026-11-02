package com.aloha.project.domain.common;

import java.time.LocalDateTime;

import com.aloha.project.domain.users.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hazard_comments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor 
public class HazardComments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hazard_no", nullable = false)
    private HazardReports hazard; // 어느 위험글의 댓글인지 연결

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_no", nullable = false)
    private Users user; // 댓글 작성자 (단장 또는 조종사)

    @Column(nullable = false, length = 1000)
    private String content; // 댓글 본문

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}