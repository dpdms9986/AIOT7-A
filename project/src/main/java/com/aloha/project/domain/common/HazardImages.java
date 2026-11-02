package com.aloha.project.domain.common; 

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hazard_images")
@Getter 
@Setter
@Builder 
@AllArgsConstructor 
@NoArgsConstructor
public class HazardImages {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hazard_no", nullable = false)
    private HazardReports hazard; // 어떤 위험 구역의 사진인지 연결

    @Column(nullable = false, length = 255)
    private String originName; // 원본 파일명 (예: 전신주.jpg)

    @Column(nullable = false, length = 255)
    private String saveName; // 서버에 저장된 파일명 (UUID 중복 방지)

    @Column(nullable = false, length = 500)
    private String filePath; // 브라우저가 읽을 웹 경로 (/uploads/hazards/...)

    private Long fileSize; // 파일 크기

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}