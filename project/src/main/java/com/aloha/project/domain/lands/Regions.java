package com.aloha.project.domain.lands;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter 
@Setter
@Builder 
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "regions")
public class Regions {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @Column(nullable = false, unique = true, length = 20)
    private String regionCode; // 법정동/행정동 코드 (예: 3011059000)

    @Column(nullable = false, length = 50)
    private String sido; // 시/도 (예: 대전광역시)

    @Column(length = 50)
    private String sigungu; // 시/군/구 (예: 동구)

    @Column(nullable = false, length = 50)
    private String eupmyeondong; // 읍/면/동 (예: 어남동)

    @Column(nullable = false)
    private Double centerLat;

    @Column(nullable = false)
    private Double centerLng;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // 폐지 여부 (기본값 true 지정)
}