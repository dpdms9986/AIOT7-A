package com.aloha.project.domain.lands;

import java.time.LocalDateTime;

import com.aloha.project.domain.users.Teams;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // Lands.java 내부
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_no")
    private Teams team; // 어느 방제단의 농지인지 매핑

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

    @Column(columnDefinition = "TEXT")
    private String polygonCoordinates;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // Lands.java 및 WorkAssignments.java 내부에 각각 추가
    public void updateStatus(String status) {
    this.status = status;
}
}
