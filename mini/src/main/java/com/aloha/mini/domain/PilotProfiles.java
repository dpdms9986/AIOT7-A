package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 5초 주기 위치 폴링과 15초 통신 지연 감지를 위해 currentLat, currentLng, lastLocationUpdatedAt 필드가 포함

@Entity
@Table(name = "pilot_profiles")
@Getter
@Setter 
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PilotProfiles {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_no", nullable = false, unique = true)
    private Users user; // Users의 PK(no) 참조

    @Column(nullable = false, length = 30)
    private String skillLevel; // 숙련도 (BEGINNER, INTERMEDIATE, EXPERT)

    @Column(length = 20)
    private String phone; // 조종사 연락처 - USER_011

    @Column(length = 50)
    private String licenseNumber; // 조종 자격증 번호

    private Double currentLat; // 실시간 현재 위도 (5초 폴링용)

    private Double currentLng; // 실시간 현재 경도 (5초 폴링용)

    private LocalDateTime lastLocationUpdatedAt; // 마지막 GPS 수신 시각 (15초 지연 체크용)
}
