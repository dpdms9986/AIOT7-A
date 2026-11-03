package com.aloha.project.domain.users;

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

    @Column(nullable = false)
    private String teamName;
    
    @Column(unique = true, nullable = false)
    private String inviteCode;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_no")     // nullable = true 유지!
    private Users leader;               // 방제단장(ROLE_LEADER) 객체 직접 매핑 
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
