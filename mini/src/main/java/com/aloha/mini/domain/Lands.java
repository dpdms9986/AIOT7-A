package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lands")
@Getter 
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor 
@Builder
public class Lands {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_no", nullable = false)
    private Regions region; // 어느 행정구역에 속하는지 매핑

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double areaSize;

    @Column(nullable = false, length = 20)
    private String status = "READY"; // READY, ASSIGNED, IN_PROGRESS, COMPLETED

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Lands.java 및 WorkAssignments.java 내부에 각각 추가
    public void updateStatus(String status) {
    this.status = status;
}
}
