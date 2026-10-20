package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter 
@Setter
@Builder
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "teams")
public class Teams {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    private String teamName;
    
    @Column(unique = true)
    private String inviteCode;
    
    @Column(name = "leader_no")
    private Long leaderNo; 
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
